# EcoSwap (Eco Swape) — Project Summary

**What it is:** a free, campus-only platform where university students swap, give away and share upcycling materials. There is no buying, selling or payment of any kind. There are two access levels, USER and ADMIN, and administrators approve every new account and every new or significantly changed listing.

**Built with:** Java 21, Spring Boot 3.5 (MVC, Security, Data JPA/Hibernate, Validation, Mail), Flyway, MySQL 8, Thymeleaf, HTML5/CSS3, vanilla JavaScript ES modules, Maven, JUnit 5 and Mockito.

## Delivered

| Area | Highlights |
|---|---|
| Homepage | Reproduces the approved reference: floating navigation, hero with 3D scene and category cards, live statistics strip, campus landscape. Below it: featured items, categories, how it works, community activity, impact and a call to action. |
| Students | Register (pending approval, e-mail verification), log in (remember-me), reset password, post and edit listings with 1–6 photos, Explore (search, filters, sort, pagination), item details, swap, giveaway and upcycling requests, accept, decline, cancel, withdraw, two-sided completion, saved items, history, notifications (live badges), profile, support tickets, reports |
| Admin | Overview (13 KPIs and 3 charts), registration and listing approval queues with reasons, users (suspend or reactivate), listings (suspend or archive, history), exchange monitoring, reports (resolve with suspension, dismiss), support (reply, status), categories, read-only audit log |
| Backend | 80 REST endpoints. Services own all business rules. Row locking in a fixed order prevents double reservations. A DB unique key prevents duplicate active requests. Approval history and audit trail are written transactionally. |
| Data | 14 specified tables (Flyway V1–V14) plus reference categories (V15). Constraints and indexes are documented in `documentation/DATABASE_ERD.md`. |
| Security | BCrypt, CSRF, session-id rotation, per-request account-status checks, method security, upload validation by magic bytes, files served through access-checked endpoints, CSP and security headers, login throttling, hashed single-use tokens |
| Quality | `mvn clean verify`: 35/35 tests against MySQL 8, including concurrency races. 17/17 browser end-to-end steps. 38-page route sweep. Responsive screenshots. |

## Key documents

`README.md` (setup) · `documentation/SYSTEM_ARCHITECTURE.md` · `DATABASE_ERD.md` · `API_DOCUMENTATION.md` · `USER_WORKFLOW.md` · `ADMIN_WORKFLOW.md` · `SECURITY_GUIDE.md` · `TEST_REPORT.md` · `DEVELOPMENT_LOG.md` · `FILE_MANIFEST.md` · `ECOSWAP_ALL_FILES.md` (complete merged source).

## Before a public launch

Confirm the licence of the hero and landscape artwork (taken from the reference design) or replace it. Configure SMTP and HTTPS. Set the allowed university e-mail domains. Review the remaining limitations in `TEST_REPORT.md` §5.
