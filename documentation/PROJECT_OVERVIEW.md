# EcoSwap (Eco Swape) — Project Overview

## 1. What the application is

Eco Swape is a **free, campus-only, peer-to-peer platform** for giving university items a second life. Students list things they no longer need, and other students request them. Nothing is ever bought or sold: there are no prices, carts, checkouts, payments, commissions or subscriptions.

Three kinds of listing exist:

| Type | What happens | Offered item needed? |
|---|---|---|
| **SWAP** | One item is exchanged for another | Yes, one of the requester's own approved, available items |
| **GIVEAWAY** | An item is given away for free | No |
| **UPCYCLING** | Reusable materials (cardboard, glass, fabric…) for creative projects | No |

There are exactly **two access levels**:

- **USER**: every student has one account and one dashboard. The same account posts items, requests items, receives requests and manages exchanges. There are no buyer or seller accounts.
- **ADMIN**: a separate protected dashboard for moderation. No public admin registration exists; the first admin is created from environment variables at startup.

### The three gates that keep the platform trustworthy

1. **Account approval.** A new registration is `PENDING_APPROVAL` until an admin approves it. Pending, rejected and suspended accounts cannot use protected features.
2. **Listing approval.** A new or significantly edited listing is `PENDING_APPROVAL` and hidden until an admin approves it. Admins can also reject it, request changes, suspend it or archive it.
3. **Exchange rules (no admin needed).** Requests go straight between students. The backend enforces ownership, availability, duplicate prevention and transactional reservation with row locks, so two students can never reserve the same item.

Approval state, account state and availability are **separate fields** (see `SYSTEM_ARCHITECTURE.md` §4).

## 2. Feature breakdown

### Public (logged out)
- Homepage with the reference design: floating nav, hero, category cards, live statistics, campus landscape, featured items, popular categories, how it works, community activity, impact, call to action, footer
- Explore Items: search, category, condition, listing type and pickup filters, sorting, pagination. Only approved and available items appear.
- Item details: gallery, details and owner first name. Request, save and report require login.
- How It Works, Community (privacy-respecting activity), Impact (verified figures and methodology)
- Register, Login, Forgot or Reset password, Email verification, Pending approval page

### Student (USER, ACTIVE)
| Area | Capabilities |
|---|---|
| Dashboard | Welcome, profile summary, live counters, recent notifications, quick actions |
| Post / Edit item | Name, category, description, condition, type, pickup location, 1–6 photos, optional weight; edit, resubmit and archive |
| My Listings | Status per listing (approval and availability), rejection reason, edit and resubmit, archive, incoming request count |
| My Requests | Sent requests, offered item, status, cancel pending, withdraw accepted, confirm completion |
| Received Requests | Accept or reject, confirm completion, withdraw accepted |
| Saved Items | Save, unsave, availability |
| Exchange History | Accepted and completed exchanges with the other participant |
| Notifications | All event types, mark read or all read, live unread badge (polled) |
| Profile | Name, university, photo, password change, logout |
| Reports | Report a listing or a user |
| Help & Support | FAQs, tickets with optional attachment, reference number, conversation, status |

### Administrator (ADMIN)
| Area | Capabilities |
|---|---|
| Overview | 13 live counters and three charts (registrations, new listings, completed exchanges) |
| Pending users / All users | Search, filter and paginate; approve, reject (reason), suspend (reason), reactivate |
| Pending listings / All listings | Review photos and details; approve, reject, request changes, suspend, archive; approval history |
| Exchanges | Monitor every request with filters and full detail |
| Reports | Start review, resolve (optionally suspend the listing or user), dismiss, notes |
| Support | Search and filter tickets, read, reply, change status |
| Categories | Add, edit, activate, deactivate; safe delete only when unused |
| Audit logs | Read-only filterable log of every admin action |

## 3. Pages and navigation

```
Public            /  /explore  /items/{id}  /how-it-works  /community  /impact
Auth              /login  /register  /register/pending  /forgot-password  /reset-password  /verify-email
Student (USER)    /dashboard  /items/new  /items/{id}/edit  /my/listings  /my/requests
                  /my/received-requests  /my/saved  /my/history  /notifications  /profile  /support
Admin (ADMIN)     /admin  /admin/pending-users  /admin/users  /admin/pending-listings  /admin/listings
                  /admin/exchanges  /admin/reports  /admin/support  /admin/categories  /admin/audit-logs
Errors            403 / 404 / 500 templates
```

Navigation changes with authentication state:
- **Logged out:** Home · Explore Items · How It Works · Community · Impact · Search · Log in · Join
- **Student:** the same links plus Search · Saved · Notifications (unread badge) · Exchange Requests (pending count, replacing the cart in the reference) · Profile menu. Inside the app there is a student sidebar, plus a bottom navigation bar on mobile.
- **Admin:** the public links plus Admin Dashboard and Profile menu. Inside admin pages there is an admin sidebar. Admin links are never rendered for students, and every admin route and API is also protected server-side.

## 4. Development roadmap (Part 28 order)

| Phase | Milestone | Done when |
|---|---|---|
| 1 | Foundation | Maven project, MySQL, Flyway V1–V15, entities validate, docs, git |
| 2 | Design system | Theme tokens, CSS architecture, fragments, JS core modules, wireframes |
| 3 | Homepage | Matches the reference and renders live data from MySQL |
| 4 | Authentication | Register, login, logout, remember-me, email verification, reset, bootstrap admin, status enforcement |
| 5 | Admin approvals | User and listing queues, decisions, notifications, audit log, approval history |
| 6 | Items | Storage service, post, edit, archive, photos, explore, search, filters, details |
| 7 | Exchanges | Request, accept (locked), reject, cancel, withdraw, two-sided completion |
| 8 | User UI | Dashboard and all student pages |
| 9 | Admin management | Exchanges, reports, support, categories, audit logs, charts |
| 10 | Integration | Every screen wired to real APIs |
| 11 | Testing | `mvn clean verify` green, browser end-to-end runs, responsive screenshots, TEST_REPORT |
| 12 | Packaging | Merged source, manifest, ZIP verified |

## 5. Decisions, deviations and risks

| Topic | Decision |
|---|---|
| `condition` column | It's a MySQL reserved word, so the column is `item_condition` (entity field `condition`). |
| Extra columns | `users.status_reason`; `categories.slug/icon_key/display_order`; `items.version` (optimistic locking); `exchange_requests.status_reason/completed_at/active_request_key`; `notifications.link_url`; `support_tickets.reference_code` and attachment metadata. Each supports a stated requirement. |
| Duplicate active requests | MySQL has no partial unique index, so a stored generated column (`active_request_key`) plus a UNIQUE key enforces "one PENDING/ACCEPTED request per requester and item" in the database, in addition to the service check. |
| Concurrency | Accepting a request locks the request row and both item rows (`SELECT … FOR UPDATE`, in id order to avoid deadlocks). Completion confirmation locks the request row. |
| Withdrawing accepted exchanges | Not in the spec, but needed so reserved items can't get stuck. Either participant can withdraw an ACCEPTED exchange with a reason, and both items return to AVAILABLE. |
| Item delete | `DELETE /api/items/{id}` archives (soft delete) so exchange history stays intact. |
| Admin vs student features | Admin accounts moderate; they don't post or request items. |
| Statistics | Only real database counts. Weight is "recorded reused weight" from owner-entered values. No CO₂ estimate is shown because no reliable factor is available; the Impact page explains this. |
| Real-time | The notification badge and request counter refresh by lightweight polling (60 s, paused in hidden tabs). This avoids extra infrastructure. |
| Hero / landscape artwork | Taken from the supplied reference design. If that artwork isn't licensed for production, replace `static/img/hero-scene.webp` and `landscape.webp` with original art. |
| Email | SMTP via `SPRING_MAIL_*` env vars. Without SMTP, links are written to the log (development only). |
| Tests | No Docker daemon in the build environment, so integration tests run against a local MySQL 8 database (`ecoswap_test`) instead of Testcontainers. |
| Fonts | Manrope (headings) and Inter (body) are self-hosted, with no external CDN. |
