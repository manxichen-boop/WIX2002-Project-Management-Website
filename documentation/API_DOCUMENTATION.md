# API Documentation

Base URL: `http://localhost:8080`. All `/api` endpoints exchange JSON unless marked *multipart*.

## Conventions

| Topic | Rule |
|---|---|
| Authentication | Session cookie `ECOSWAP_SESSION`, created by `POST /api/auth/login`. Optional remember-me cookie `ECOSWAP_REMEMBER`. |
| CSRF | Every `POST/PUT/PATCH/DELETE` must send the header `X-CSRF-TOKEN`, whose value comes from `<meta name="_csrf">` on any page. `js/core/api.js` does this automatically. Missing or invalid token → `403 CSRF_INVALID`. |
| Access | **Public**: no login. **User**: an ACTIVE student. **Admin**: an ACTIVE administrator. **Any**: either role. Checked by URL rules and again by `@PreAuthorize` and ownership checks in services. |
| Pagination | `?page=0&size=20` → `{content:[…], page, size, totalElements, totalPages}` |
| Times | ISO-8601 UTC instants, e.g. `2026-11-13T01:23:45.123Z` |
| Errors | `{status, code, message, fieldErrors:[{field, message}], timestamp}` |

Common error codes: `VALIDATION_FAILED` (400), `UNAUTHENTICATED` (401), `ACCOUNT_INACTIVE` (401, the session ended because the account was suspended), `FORBIDDEN` / `CSRF_INVALID` (403), `NOT_FOUND` (404), `INVALID_STATE`, `DUPLICATE_REQUEST`, `OWN_ITEM`, `NOT_AVAILABLE`, `CONCURRENT_UPDATE`, `CONFLICT` (409), `FILE_TOO_LARGE` (413), `TOO_MANY_ATTEMPTS` (429).

---

## Authentication

| Method & path | Access | Body / params | Result |
|---|---|---|---|
| `POST /api/auth/register` | Public | `{fullName, email, password, confirmPassword, university, acceptTerms}` | `201 {message, email, verificationEmailSent}`. Creates a USER with status PENDING_APPROVAL. Any `role` or `status` fields are ignored. `409 EMAIL_TAKEN`. |
| `POST /api/auth/login` | Public | `{email, password, rememberMe}` | `200 {redirectUrl, user}`. Errors: `401 INVALID_CREDENTIALS`, `403 ACCOUNT_PENDING / ACCOUNT_REJECTED / ACCOUNT_SUSPENDED / EMAIL_NOT_VERIFIED`, `429 TOO_MANY_ATTEMPTS` |
| `POST /api/auth/logout` | Any | none | `204`. The session and remember-me cookie are cleared. |
| `GET /api/auth/me` | Any | none | `{id, fullName, email, university, role, accountStatus, emailVerified, avatarUrl}` |
| `GET /api/auth/verify-email?token=` | Public | none | `{result: VERIFIED / ALREADY_VERIFIED / EXPIRED / INVALID}`. The e-mailed link opens the page `/verify-email`. |
| `POST /api/auth/resend-verification` | Public | `{email}` | Generic `200` (never reveals whether the account exists) |
| `POST /api/auth/forgot-password` | Public | `{email}` | Generic `200`. Sends a 30-minute single-use link. |
| `POST /api/auth/reset-password` | Public | `{token, password, confirmPassword}` | `200`, or `400 TOKEN_INVALID` |

## Items

| Method & path | Access | Body / params | Result |
|---|---|---|---|
| `GET /api/items` | Public | `q, category (slug), condition, type (SWAP/GIVEAWAY/UPCYCLING), location, sort (newest/oldest/name), page, size≤48` | Page of item cards, **APPROVED + AVAILABLE only**. `saved` is filled in for logged-in students. |
| `GET /api/items/{id}` | Public* | none | Full details, including `viewer` (`canRequest`, `requestBlockedReason`, `saved`, …). *Unapproved listings return 404 except to their owner and admins. |
| `POST /api/items` | User | *multipart*: `name, categoryId, description, condition, listingType, pickupLocation, approximateWeight?` and `photos` (1–6 JPG/PNG/WebP, ≤5 MB each) | `201 {id, message}`. Status is PENDING_APPROVAL. Admins are notified. |
| `PUT /api/items/{id}` | Owner | *multipart*: same fields, new `photos`, `removeImageIds` | `200`. Significant changes to an approved listing, or any edit of a rejected or change-requested one, set it to PENDING_APPROVAL. |
| `DELETE /api/items/{id}` | Owner | none | Archives the listing (soft delete) and closes its pending requests. `409` if reserved or completed. |
| `GET /api/items/mine` | User | `tab=active/archived, page, size` | `{item, rejectionReason, pendingRequests, editable, archivable}` |
| `GET /api/items/mine/available` | User | none | The caller's approved, available items (offers for swaps) |
| `GET /api/items/map` | Public | none | Newest 300 APPROVED + AVAILABLE listings that have a map pin (`pickupLatitude`, `pickupLongitude`, `categoryIconKey`). Used by `/pickup-map`. |
| `GET /api/categories` | Public | none | Active categories |

`POST`/`PUT /api/items` also accept the optional map pin `pickupLatitude` (−90…90) and `pickupLongitude` (−180…180). Send both or neither (`400` otherwise); they are stored with 6 decimals.

## Exchange requests

| Method & path | Access | Body | Rules |
|---|---|---|---|
| `POST /api/items/{itemId}/requests` | User | `{offeredItemId?, message?}` | Item must be APPROVED + AVAILABLE and not the caller's own. No active duplicate (also enforced by a DB unique key). SWAP needs an `offeredItemId` the caller owns that is APPROVED + AVAILABLE. GIVEAWAY and UPCYCLING must not include one. |
| `GET /api/requests/sent` | User | `status?, page, size` | Requests the caller made |
| `GET /api/requests/received` | User | `status?, page, size` | Requests for the caller's items |
| `GET /api/requests/history` | User | `page, size` | ACCEPTED and COMPLETED exchanges, either side |
| `GET /api/requests/{id}` | Participant | none | 404 for non-participants |
| `PATCH /api/requests/{id}/accept` | Owner | none | Locks the item rows, then the request row, and re-checks. Reserves the item (and the offered item). Closes conflicting pending requests. |
| `PATCH /api/requests/{id}/reject` | Owner | `{reason?}` | PENDING → REJECTED |
| `PATCH /api/requests/{id}/cancel` | Participant | `{reason?}` | PENDING (requester) → CANCELLED. ACCEPTED (either side, reason required) → CANCELLED, and the items return to AVAILABLE. |
| `PATCH /api/requests/{id}/confirm-completion` | Participant | none | Records this side's confirmation. When both sides have confirmed → COMPLETED, items COMPLETED. |

Each request view includes `actions {accept, reject, cancel, withdraw, confirm}`, computed on the server for the caller. The participants' `contactEmail` is only present once the request is ACCEPTED or COMPLETED.

## Chat between the two students of an exchange (role USER)

| Method & path | Access | Body / params | Result |
|---|---|---|---|
| `GET /api/chats` | User | none | Conversations: accepted or completed exchanges the caller takes part in, plus closed ones that already have messages. Each has `other`, `item`, `offeredItem`, `lastMessage`, `unread`, `canSend` and the handover flags. |
| `GET /api/chats/unread-count` | User | none | `{unread}` |
| `GET /api/chats/{requestId}` | Participant | `after?` (message id) | `{conversation, messages, hasOlder}`: the newest 50, or only messages after `after` (polling). Marks the other student's messages as read. 404 for anyone else, including admins (403 on the route). |
| `GET /api/chats/{requestId}/older` | Participant | `before` | The 50 messages before that id |
| `POST /api/chats/{requestId}/messages` | Participant | `{body}` (1–2000 chars) | `201` message. Only while the exchange is ACCEPTED or COMPLETED (`409 CHAT_NOT_OPEN` / `CHAT_CLOSED`). 20 messages per minute per student (`409 SLOW_DOWN`). The other student gets one notification per run of unread messages. |

`GET /api/notifications/counts` now also returns `unreadMessages` (header and bottom-navigation badges).

## Favourites, notifications, dashboard, profile, reports, support

| Method & path | Access | Notes |
|---|---|---|
| `GET /api/favorites` · `POST /api/favorites/{itemId}` · `DELETE /api/favorites/{itemId}` | User | Saving is idempotent; the DB has a unique (user, item) key |
| `GET /api/notifications?unreadOnly=&page=&size=` | Any | Newest first |
| `GET /api/notifications/counts` | Any | `{unread, pendingReceivedRequests}`. The header badges poll this every 60 s. |
| `PATCH /api/notifications/{id}/read` · `PATCH /api/notifications/read-all` | Any | Only the caller's own notifications |
| `GET /api/dashboard/summary` | User | Profile, 8 counters, requests awaiting an answer, recent notifications |
| `GET /api/profile` · `PUT /api/profile {fullName, university}` | Any | E-mail and role cannot be changed |
| `POST /api/profile/photo` (*multipart* `photo`) · `DELETE /api/profile/photo` | Any | Image validated like listing photos |
| `PATCH /api/profile/password {currentPassword, newPassword, confirmPassword}` | Any | Invalidates remember-me cookies |
| `POST /api/reports {itemId? , reportedUserId?, reason, description?}` | User | Reasons: `INAPPROPRIATE_LISTING, MISLEADING_DESCRIPTION, SUSPICIOUS_USER, PROHIBITED_CONTENT, INAPPROPRIATE_BEHAVIOUR, OTHER`. Duplicate open reports → 409. |
| `POST /api/support/tickets` (*multipart* `subject, message, attachment?`) | User | Returns `referenceCode` (e.g. `ES-7K2QX9`). The attachment can be an image or a PDF up to 5 MB. |
| `GET /api/support/tickets/mine` · `GET /api/support/tickets/{id}` · `POST /api/support/tickets/{id}/reply {message}` | User | Owner only |
| `GET /api/impact/summary` | Public | Verified counts and recorded weight, by type and by category |
| `GET /api/community/activity` | Public | Recently completed exchanges, anonymised |

## Administration (role ADMIN)

| Method & path | Notes |
|---|---|
| `GET /api/admin/dashboard` | 13 KPIs, three 8-week series, recent audit entries |
| `GET /api/admin/users?status=&q=` · `GET /api/admin/users/pending` | Students only |
| `PATCH /api/admin/users/{id}/approve` · `/reject {reason}` · `/suspend {reason}` · `/activate` | Each writes approval history and an audit row, and notifies and e-mails the student |
| `GET /api/admin/items?approval=&availability=&q=` · `GET /api/admin/items/pending` · `GET /api/admin/items/{id}` | `{id}` includes photos, owner and approval history |
| `PATCH /api/admin/items/{id}/approve` · `/reject {reason}` · `/request-changes {reason}` · `/suspend {reason}` · `/archive {reason}` | Negative decisions close pending requests and cancel reservations |
| `GET /api/admin/exchanges?status=&q=` · `GET /api/admin/exchanges/{id}` | Read-only monitoring |
| `GET /api/admin/reports?status=` · `PATCH /api/admin/reports/{id}/review` · `/resolve {notes, suspendListing, suspendUser}` · `/dismiss {reason}` | Resolving can suspend the listing and/or the student |
| `GET /api/admin/support/tickets?status=&q=` · `GET …/{id}` · `POST …/{id}/reply {message}` · `PATCH …/{id}/status {status}` | Statuses: OPEN, IN_PROGRESS, WAITING_FOR_USER, RESOLVED, CLOSED |
| `GET /api/admin/categories` · `POST` · `PUT /{id}` · `PATCH /{id}/activate` · `PATCH /{id}/deactivate` · `DELETE /{id}` | Delete only works for categories that have never been used |
| `GET /api/admin/audit-logs?action=&targetType=` | Read-only |

## Media (controlled file access)

| Path | Who can see it |
|---|---|
| `GET /media/items/{imageId}` | Anyone if the listing is approved; otherwise its owner and admins (others get 404) |
| `GET /media/profiles/{userId}` | Logged-in users |
| `GET /media/support/{ticketId}` | Ticket owner and admins |

## Example

```bash
# 1. get a session + CSRF token
curl -c jar -s http://localhost:8080/login | grep -o 'name="_csrf" content="[^"]*"'
# 2. log in
curl -b jar -c jar -H 'Content-Type: application/json' -H "X-CSRF-TOKEN: $TOKEN" \
     -d '{"email":"student@siswa.um.edu.my","password":"Swap2026x","rememberMe":false}' \
     http://localhost:8080/api/auth/login
# 3. search
curl -b jar 'http://localhost:8080/api/items?q=calculator&type=GIVEAWAY'
```
