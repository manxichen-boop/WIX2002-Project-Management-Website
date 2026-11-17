# Security Guide

How each Part 17 rule is implemented, and where.

| # | Rule | Implementation |
|---|---|---|
| 1–2 | Public registration only creates USER accounts | `User.newStudent()` hard-codes `USER` + `PENDING_APPROVAL`. The request DTO has no role field, so extra JSON fields are ignored (tested: `publicRegistrationCannotEscalateRole`). |
| 3 | Admins are created securely | `AdminBootstrapRunner` creates them from environment variables, once, and never promotes an existing account. No credentials are in the code. |
| 4–6 | Pending, rejected and suspended users are blocked | Login checks the password **first**, then the status (`AuthService.authenticate`). `AccountStatusFilter` re-reads the status on every request and ends the session immediately. `AuthenticatedUser.isEnabled()` blocks remember-me logins too. |
| 7–8, 12 | Only admins approve accounts and listings and use admin APIs | URL rules (`/admin/**`, `/api/admin/**` → `ROLE_ADMIN`) **and** `@PreAuthorize("hasRole('ADMIN')")` on every admin service and controller. `@EnableMethodSecurity` is on. |
| 9 | Users edit only their own listings | `ItemService.ownedItem()` → 403 (tested) |
| 10 | No requesting your own item | `ExchangeService.create` → `OWN_ITEM` |
| 11 | Only participants confirm | `confirmCompletion` → 404 for non-participants |
| 13 | Backend validation | Bean Validation on every DTO plus service-level rules. The frontend checks are only for convenience. |
| 14 | No SQL injection | Only JPA/JPQL with bound parameters and Criteria specifications. LIKE wildcards in user input are escaped (`Texts.likePattern`). |
| 15 | No unauthorised file access | Files are outside the web root and served only via `/media/**` with visibility checks. Random names, path normalisation and a root check prevent traversal (tested). |
| 16 | Sensitive data | BCrypt (cost 12) passwords. Verification and reset tokens are stored only as SHA-256 hashes, single-use, with expiry. E-mails are shown only to admins and to exchange participants after acceptance. Public activity is anonymised. |
| 17 | Admin actions recorded | `AuditService.log` writes in the same transaction as the action (`Propagation.MANDATORY`). The entity is `@Immutable` and there is no update or delete API. |

## Session and request protection

- **CSRF**: Spring Security synchroniser tokens. Pages expose the token in `<meta>`, and `api.js` sends `X-CSRF-TOKEN`. Logout is POST plus CSRF.
- **Session fixation**: the session id changes and the CSRF token rotates on login.
- **Cookies**: `HttpOnly`, `SameSite=Lax`, and `Secure` in the prod profile. The remember-me token (14 days) embeds the password hash, so changing the password revokes it.
- **Brute force**: 5 failed logins per e-mail lock it for 15 minutes. Password-reset and verification e-mails are rate-limited too. Unknown e-mails take the same time as wrong passwords (a dummy BCrypt comparison).
- **Account enumeration**: forgot-password and resend-verification always return the same message. (Registration does reveal a duplicate e-mail, because the spec requires that error.)
- **Headers**: `Content-Security-Policy` (scripts only from self, no inline script), `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`.
- **XSS**: Thymeleaf escapes by default. The JS `html` tagged template escapes every interpolated value.
- **Uploads**: the type is detected from magic bytes (JPEG/PNG/WebP, and PDF for support attachments only) and the extension must match. Limits are 5 MB per file, 6 photos per listing and 32 MB per request. The client's file name is only stored, sanitised, as metadata.
- **Concurrency**: item and request rows are locked in a fixed order (`SELECT … FOR UPDATE`), and a database unique key prevents duplicate active requests.

## Operational guidance

Never commit `.env`. Rotate `ECOSWAP_REMEMBER_ME_KEY` if it leaks (this logs out remember-me sessions). Remove `ECOSWAP_ADMIN_PASSWORD` after the first start. Use HTTPS in production. Restrict MySQL to the application host. Review the audit log regularly.
