# Development Log

Progress is recorded after each phase (Part 29). Newest phase at the bottom.

## Phase 1 — Project Foundation ✅

**Completed:** Maven project (Spring Boot 3.5.6, Java 21), MySQL 8 configuration with dev/prod/test profiles, Flyway V1–V14 exactly as specified plus V15 reference categories, every entity, enum and repository, Maven wrapper, `.env.example`, setup scripts (SQL, bash, PowerShell), git repository, and planning documentation.

**Files created:** `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/`, `.gitignore`, `.env.example`, `scripts/*`, `src/main/resources/application*.yml`, `src/test/resources/application-test.yml`, `db/migration/V1…V15`, 59 Java files (entities, enums, repositories, common layer), `documentation/PROJECT_OVERVIEW.md`, `USER_WORKFLOW.md`, `ADMIN_WORKFLOW.md`, `DATABASE_ERD.md`, `SYSTEM_ARCHITECTURE.md`.

**Tests performed:** Application booted against MySQL 8.0.46. All 15 migrations applied and Hibernate `validate` passed for every entity.

**Errors remaining:** none.

**Next:** Phase 2 design system.

## Phase 2 — Design System ✅

**Completed:** Central theme (official palette as CSS variables, derived accessible shades), self-hosted Manrope, Inter and Caveat, `app.css` importing theme → base → layout → components → pages → home → dashboard → admin → responsive, the master layout `layout/base.html` (public, student and admin shells), fragments (head, header, footer, user-sidebar, admin-sidebar, bottom-navigation, toast-container, modal-container, ui helpers), JS core modules (`api.js` with CSRF and errors, `ui.js` with dialogs, toasts, states, pagination and badges, `validation.js`, `auth.js`), `app.js` page loader, a Lucide icon sprite (83 icons), six SVG wireframes, and `UI_UX_GUIDELINES.md`. Security foundation: principal, account-status filter, remember-me, JSON-aware entry points and the filter chain.

**Fixed during review:** `th:replace` takes precedence over `th:if` on the same tag, so conditional fragments are now wrapped in `th:block`. Before the fix, the student bottom nav was visible to logged-out visitors. Bottom-nav "post" icon specificity.

## Phase 3 — Homepage ✅

**Completed:** Auth-aware floating navbar (Log in / Join for visitors; the cart is replaced by Exchange Requests). Hero, 3D illustration and floating category cards linked to Explore. Statistics strip with **live** values (active members, completed exchanges, items reused, recorded kg). Campus landscape with the tagline. Featured items (newest approved, with an empty state when none). Popular categories with live counts. How it works and the three listing types. Anonymised community activity. Impact band. Final CTA. Footer.

**Tests performed:** Real app running on MySQL 8. Screenshots at 1447 px (the reference size), 820 px and 390 px saved in `documentation/screenshots`. Anonymous `/dashboard` redirects to login; anonymous `/api/admin/*` returns a JSON 401.

**Known limits:** The hero and landscape artwork is taken from the supplied reference. Replace it if it isn't licensed for production.

## Phase 4 — Authentication ✅

**Completed:** Registration (validation, optional allowed-domain list, BCrypt cost 12, USER + PENDING_APPROVAL, verification e-mail, admin notification). JSON login that checks the password before revealing account status, with a constant-time path for unknown e-mails, session-id rotation, CSRF token rotation and optional remember-me. Logout (CSRF-protected). Single-use, hashed e-mail verification (48 h) and password reset (30 min) tokens, with resend. Per-e-mail login throttling (5 attempts, 15-minute lock). Mail service using SMTP when configured, otherwise the log. Secure admin bootstrap from environment variables (never promotes existing accounts). Account-status filter that ends sessions of suspended or rejected users on their next request. Auth pages: login, register, pending approval / verification result, forgot password and reset password.

**Files:** `auth/{dto,service,controller,security,entity,repository}`, `notification/*` (service, API, counts), `web/AuthPageController`, `templates/pages/auth/*`, `js/pages/{login,register,forgot-password,reset-password,account-status}.js`.

**Tests performed (HTTP against the running app + MySQL):** register 201; duplicate e-mail 409; invalid input 400 with field errors; pending login 403 ACCOUNT_PENDING; wrong password and unknown e-mail both 401 INVALID_CREDENTIALS; admin login → /admin; `/me`; logout without CSRF 403; logout 204; `/me` after logout 401; verification link (and reuse); reset with a real token; token reuse rejected; throttling 429 after 5 failures. DB rows checked.

**Fixed:** `.field` rows stretching inside two-column form rows; auth illustration edge artefacts.

## Phases 5–7 — Admin approvals, item management, exchange system ✅

**Completed:**
- **Storage:** `StorageService` interface, local implementation outside the web root (random names, traversal-safe), magic-byte type detection (JPEG/PNG/WebP, plus PDF for attachments), size and count limits, rollback-safe file clean-up, and `/media/**` endpoints with per-file visibility checks, `nosniff` and cache rules.
- **Items:** post (1–6 photos), edit (add or remove photos; significant edits of approved listings return to review), resubmit after rejection or change requests, archive (soft delete), My Listings, available-for-swap list, Explore search (text, category, condition, type, pickup, sort, pagination; approved and available only), and item details with viewer-specific actions.
- **Admin approvals:** registration queue (approve, reject with reason), suspend, reactivate; listing queue (approve, reject, request changes, suspend, archive) with approval history, audit log, notifications and e-mails, all in one transaction.
- **Exchanges:** create with ownership, availability, duplicate and swap-offer rules; accept with row locks and conflicting pending requests auto-closed; reject; cancel (pending) or withdraw (accepted, releases items); two-sided completion; history. Contact e-mails are shown to participants only after acceptance.
- **Favourites** and student **reports** (with duplicate prevention).
- **UI:** Explore, item details (request dialog with offer picker, save, report), post/edit item, My Listings, and admin pending users, all users, pending listings and all listings.

**Tests performed (HTTP + MySQL):** 61 scenario checks over three runs, all passing after fixes. The highlights: pending items are hidden from Explore, the media endpoint and other students. Fake image upload, missing photos, own-item requests, swaps without an offer, offering another student's item, duplicate requests, giveaway offers, a requester accepting, double confirmation, an outsider confirming, cross-user edit or archive, students calling admin APIs, and path traversal are all rejected correctly. Acceptance closes conflicting requests; the exchange completes only after both confirmations. Database rows, audit (8) and approval history (8) were checked.

**Fixed:** item endpoints now accept URL-encoded forms as well as multipart (previously 415). The JS `html` helper now returns nest-safe markup, which prevented double-escaped badges.

## Phases 8–10 — Remaining UI, admin management, integration ✅

**Completed:**
- **Student UI:** dashboard (8 live counters, requests awaiting an answer, recent notifications), My Requests, Received Requests (accept, decline, confirm, withdraw), Saved Items, Exchange History, Notifications (filters, mark read or all read), Profile (details, photo, password, logout), Help & Support (FAQ, tickets with attachment and reference code, conversation, replies), plus public How It Works with community guidelines, Community (anonymised activity) and Impact (verified figures and methodology).
- **Admin:** overview (13 KPIs in three groups, three weekly bar charts with tooltips and table views, recent actions), exchange monitoring (read-only with details), reports (review, resolve with optional listing/student suspension, dismiss), support (reply and status), categories (create, edit, activate or deactivate, delete unused only), audit logs (filterable, read-only).
- **APIs:** `/api/dashboard/summary`, `/api/profile/**`, `/api/impact/summary`, `/api/community/activity`, `/api/support/tickets/**`, `/api/admin/{dashboard,exchanges,reports,support,categories,audit-logs}`.
- **Integration:** every screen loads from and writes to the REST API. There are no static request statuses or fake numbers.

**Tests performed:** Real-Chrome sweep of 38 page loads across visitor, student and admin roles (logged in through the UI): all 200 with no console or HTTP errors, except the intended 403 for a student on `/admin`. Screenshots reviewed.

**Fixed:** the admin sidebar crashed on `/notifications` and `/profile` because queue counts were only loaded under `/admin`. SpEL method references (unsupported) in the impact template were moved into Java. Another `th:if` + `th:replace` collision on the community page.

## Phase 11 — Testing ✅

**Completed:** 35 automated tests (JUnit 5 + Mockito unit tests; Spring Boot + MockMvc integration tests on MySQL 8, including real thread races), a 17-step browser end-to-end run through the UI, a 38-page multi-role route sweep, and responsive screenshots. Results and limitations: `TEST_REPORT.md`.

**Defect found and fixed:** a deadlock between concurrent acceptances, caused by locking request then item. The order is now items (ascending id) then request, and lock conflicts map to `409 CONCURRENT_UPDATE`. Also polished: mobile stat grid and the message for exchanged items.

## Phase 12 — Packaging ✅

**Completed:** `scripts/build-deliverables.py` regenerates `documentation/FILE_MANIFEST.md` (all files with responsibilities) and `ECOSWAP_ALL_FILES.md` (every text file, verified byte-identical to disk; binary assets listed). README, installation, deployment, security, API, test report and project summary written. `EcoSwap_Complete_Project.zip` built without `target/`, `.git/`, `.env` or uploaded files, then verified by extracting it and building the extracted copy.

**Remaining limitations:** see `TEST_REPORT.md` §5 (Testcontainers/SMTP/cross-browser not exercised here; artwork licence to confirm).
