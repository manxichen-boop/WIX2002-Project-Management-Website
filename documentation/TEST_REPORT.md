# Test Report

**Environment:** Ubuntu 24.04 · OpenJDK 21.0.12 · MySQL Server 8.0.46 · Maven 3.9.11 (wrapper) · Google Chrome via Playwright for browser runs.
**Date:** 13 November 2026 · **Build:** `ecoswap-1.0.0`

## 1. Automated suite — `mvn clean verify` ✅ 38 / 38 passed

| Test class | Type | Tests | What it proves |
|---|---|---|---|
| `AuthAndApprovalIntegrationTest` | Integration (MySQL) | 5 | Registration creates USER + PENDING_APPROVAL with a BCrypt hash, a verification token and admin notifications. Duplicate e-mail → 409; weak password → 400. A `role`/`status` escalation attempt is ignored. Pending login → 403. Approve → login works. Suspend → the existing session is rejected on the next request (`ACCOUNT_INACTIVE`) and login is refused. Reactivate → login works. Rejected students see the reason. Audit and approval rows are written. |
| `SecurityIntegrationTest` | Integration | 7 | Anonymous users are blocked from protected APIs (401) and pages (redirect to login). Students get 403 on admin APIs and pages. A missing CSRF token → 403 `CSRF_INVALID`. Cross-user edit or archive → 403. A spoofed image upload → 400. Pending listings are hidden from others and from Explore. Wrong password and unknown e-mail give the same response. |
| `ExchangeIntegrationTest` | Integration | 6 | Full swap: accept reserves both items; double confirmation is refused; COMPLETED only after both confirm; it appears in history. All create rules (own item, missing offer, someone else's item as offer, offer on a giveaway, duplicate). Cancel, then re-request. The DB unique key blocks duplicate active requests even when the service is bypassed. Acceptance closes conflicting requests; withdrawing needs a reason and releases items. **Concurrency:** 4 owners' acceptances raced for the same item → exactly 1 succeeds and the others get clean business errors. Two swaps raced over the same offered item → exactly 1 succeeds. |
| `ExchangeServiceUnitTest` | Unit (Mockito) | 3 | Request creation trims the message and notifies the owner. Unavailable items never reach the repository. Duplicates are refused. |
| `ItemModerationRulesTest` | Unit | 5 | Initial states. A significant edit forces re-approval; a minor edit keeps it. Editing a rejected or change-requested listing resubmits it. Approval clears the reason. Reservation never changes the moderation status. |
| `StorageAndUploadTest` | Unit | 5 | Magic-byte detection (JPEG/PNG/WebP/PDF). Spoofed content, a wrong extension, PDFs as photos and oversized files are rejected. Client file names are sanitised. Storage names are random. Path traversal is blocked. |
| `PasswordAndThrottlingTest` | Unit | 3 | Password policy (including the 72-byte BCrypt limit), login lock-out and reset, token randomness and hashing. |
| `WeeklySeriesTest` | Unit | 1 | Admin chart bucketing by ISO week, including empty weeks. |
| `ChatMapAndImpactIntegrationTest` (v1.1) | Integration | 3 | Chat is closed while a request is pending and opens on acceptance; a burst of messages is one notification; unread counts and read receipts; polling with `after`; strangers get 404, admins 403, anonymous 401; empty and over-long messages → 400; HTML is stored as text; a withdrawn exchange's chat is read-only. Completing an exchange updates completed exchanges, items reused (+2 for a swap) and recorded weight immediately, and only after **both** confirm. Map pins: half or out-of-range coordinates → 400, valid pins stored with 6 decimals, only pinned listings on the public map endpoint. |

### Defect found by the tests and fixed

The concurrency tests initially failed. MySQL reported `Deadlock found when trying to get lock` when two acceptances raced, because each transaction locked its *own request row* first and then the shared item. Integrity held (only one acceptance won), but the losing request got a 500 instead of a clean answer. **Fix:** every exchange state change now locks item rows (ascending id) **before** the request row and re-checks state under the lock. Any remaining lock conflict is translated into `409 CONCURRENT_UPDATE`. The tests were re-run three more times with no failures.

## 2. HTTP scenario runs against the running app (MySQL `ecoswap_db`) ✅

The scenario script drives the real API with a session cookie and CSRF tokens, as a browser would.
- **Auth (19 checks):** register, duplicate, validation, pending login, wrong or unknown credentials, admin login and `/me`, logout with and without CSRF, e-mail verification link (twice), reset with a real token, single-use reset, throttling (429).
- **Items, approvals and exchanges (61 checks over three runs):** approval queue, reject without a reason (400), approvals, real JPEG uploads, fake image (400), missing photo (400), hidden pending items (Explore, details, media endpoint), reject and request-changes then edit and resubmit, adding and removing photos, search with filters, every request rule, acceptance closing two conflicting requests, contact e-mail revealed after acceptance, two-sided completion, favourites (idempotent), reports (duplicate blocked), students blocked from admin APIs, cross-user edit or archive (403), path traversal (400).
- After the fixes listed in `DEVELOPMENT_LOG.md` (multipart-only endpoints returned 415 for URL-encoded edits; one test used invalid data), all checks pass.

## 3. Browser end-to-end through the real UI (Chrome) ✅ 17 / 17

Client-side validation messages. Registration form → pending page. Pending login → "awaiting approval" page. **Admin approves in the UI.** The approved student logs in (remember-me ticked) and **posts an item with a real photo upload**, which shows "Pending approval". **Admin approves the listing in the review queue.** A second student finds it through Explore search and **requests it via the dialog**. The item page then shows "Requested". The owner's header badge shows 1. The **owner accepts in Received Requests and confirms the handover**. The requester sees the owner's e-mail and **confirms**, and the exchange shows **Completed** in history. Logout via the account menu. **The homepage statistics updated from live data** (4 members · 2 exchanges · 3 items · 2.7 kg). Data persists after logging out and back in.

## 4. Page and UI checks ✅

- **Route sweep:** 38 page loads as visitor, student and admin, logged in through the UI. All returned 200 with no console errors and no failed requests, except the intended 403 when a student opens `/admin`. Two defects were found and fixed (the admin sidebar on shared pages; a template conditional).
- **Responsive:** screenshots at 1447 px (reference size), 1440 px, 820 px (tablet) and 390 px (mobile) are in `documentation/screenshots`. They show the collapsed navigation, bottom navigation, Explore filter sheet, stacked statistics, card-style tables and a horizontal admin navigation strip.
- **Empty, loading and error states:** shown on a fresh database (homepage, Explore, queues). Skeletons and Retry come from `createListController`.

## 5. Not tested / known limitations (stated honestly)

| Area | Status |
|---|---|
| Testcontainers | Not used: the build environment has no Docker daemon. Integration tests run against a local MySQL 8 database (`ecoswap_test`) configured through `TEST_DB_*` variables. |
| Real SMTP delivery | Not tested (no mail server in the build environment). The logging fallback was exercised, and the e-mailed links were followed in the tests. |
| Cross-browser | Verified in Chromium/Chrome only. Firefox and Safari are not tested here, although only standard CSS/JS is used (container query units need Safari 16+ or Firefox 110+). |
| Screen-reader testing | Semantic structure, labels, focus handling and ARIA live regions were implemented and reviewed. No assistive-technology session was run. |
| Load testing | Not performed. Locking correctness is covered by the concurrency tests, not by throughput benchmarks. |
| Multi-instance deployment | In-memory sessions and login throttling are per instance (see `DEPLOYMENT_GUIDE.md`). |
| Hero and landscape artwork | Taken from the supplied reference design. Confirm the licence, or replace it with original art before public launch. |


## v1.1 browser end-to-end (Chrome, real UI) ✅ 20 / 20

Map picker on the item form (click to pin) → admin approval → pickup map lists the item and, after "locate", shows `≈ 3 min walk · 190 m` → item page shows distance and walk time → request → chat refused while pending → owner accepts → "Chat with Ben" from Received Requests → message sent with Enter → requester's header badge shows 1 → requester opens the thread → reply appears for the owner within 3 s without reloading → "Seen" receipt → owner confirms the handover in the chat → requester sees "Farah confirmed, confirm too" live → requester confirms → "Exchange completed!" → `/api/impact/summary` goes from 4 → 5 completed exchanges, 5 → 6 items reused and 5.5 → 6.9 kg → chat stays writable after completion.

Responsive check: every public, student and admin page at 375 px, 768 px and 1280 px has no horizontal overflow and no JavaScript errors.

Not testable in the build sandbox: OpenStreetMap tiles and place search (the sandbox blocks those hosts), so maps were checked there with a blank background; tiles load normally in a browser with internet access.
