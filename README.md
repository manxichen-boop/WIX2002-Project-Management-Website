# EcoSwap (Eco Swape)

A free, campus-only, peer-to-peer platform where students **swap**, **give away** and share **upcycling** materials. Nothing is ever bought or sold. Two access levels: **USER** (students) and **ADMIN**. Every new account and every new listing is approved by an administrator.

**Stack:** Java 21 · Spring Boot 3.5 · Spring Security · Spring Data JPA/Hibernate · Flyway · MySQL 8 · Thymeleaf · HTML5/CSS3 · vanilla JavaScript (ES modules) · Maven · JUnit 5 + Mockito.

**New in 1.1:** pickup maps with distance and walking time (OpenStreetMap, no API key), chat between the two students of an accepted exchange, artwork for every category, the homepage "More" flip and phone/tablet layout fixes. See [`documentation/CHANGELOG.md`](documentation/CHANGELOG.md).

More documentation lives in [`documentation/`](documentation): architecture, ERD, API, workflows, UI guidelines, security, deployment, the test report and the file manifest.

---

## 1. Install Java 21

| OS | Command |
|---|---|
| Windows | Install **Eclipse Temurin 21** from adoptium.net and tick "Set JAVA_HOME" |
| macOS | `brew install --cask temurin@21` |
| Ubuntu/Debian | `sudo apt install openjdk-21-jdk` |

Check it with `java -version` (it should report 21.x).

## 2. Maven

Nothing to install: use the bundled wrapper (`./mvnw` on Linux/macOS, `mvnw.cmd` on Windows). It downloads Maven 3.9 the first time.

## 3. Install MySQL 8

Windows: MySQL Installer (Server 8.0). macOS: `brew install mysql@8.0 && brew services start mysql@8.0`. Ubuntu: `sudo apt install mysql-server-8.0`.

## 4. Create the database

Edit `scripts/setup-database.sql` and replace `CHANGE_ME` with a strong password. Then run:

```bash
mysql -u root -p < scripts/setup-database.sql
```

This creates `ecoswap_db` (application) and `ecoswap_test` (automated tests), plus the user `ecoswap`.

## 5. Configure the environment

```bash
cp .env.example .env            # Windows: Copy-Item .env.example .env
```

Fill in at least `DB_PASSWORD`, `ECOSWAP_ADMIN_EMAIL`, `ECOSWAP_ADMIN_PASSWORD` and `ECOSWAP_REMEMBER_ME_KEY` (any long random string). Every setting is described in `.env.example`. Never commit `.env`.

## 6. Database migration

Nothing to run by hand. **Flyway** applies `src/main/resources/db/migration/V1…V15` automatically at startup, and Hibernate then validates the schema (`ddl-auto=validate`). V15 inserts the reference categories. There is no sample data and there are no fake accounts.

## 7. Start the application

```bash
./scripts/run-local.sh                 # Linux / macOS
.\scripts\run-local.ps1                # Windows PowerShell
```

Then open <http://localhost:8080>. (The scripts load `.env` and run `mvnw spring-boot:run`.)

## 8. First administrator

There is no public admin sign-up. On startup, if no account exists for `ECOSWAP_ADMIN_EMAIL`, an ACTIVE administrator is created with `ECOSWAP_ADMIN_PASSWORD` (8+ characters, with a letter and a number). Log in at `/login`; you'll land on `/admin`. Once the account exists, remove `ECOSWAP_ADMIN_PASSWORD` from `.env`. Existing student accounts are never promoted.

## 9. Student registration

Students register at `/register` with their university e-mail. Set `ECOSWAP_ALLOWED_EMAIL_DOMAINS` to limit which domains are accepted. The account is created as **PENDING_APPROVAL**, a verification link is sent, and the administrators are notified.

## 10. Admin approval

Go to **Admin → Pending registrations** and choose Approve or Reject (with a reason). Approved students receive a notification and an e-mail and can log in. Admins can suspend or reactivate accounts under **All users**. Every decision is recorded in **Audit logs**.

## 11. Item submission

Students post at **Post an item** (1–6 photos) and can pin the pickup point on the map (search, tap the map or use their location). Pinned listings appear on **Pickup map** (`/pickup-map`), and item pages show the distance and an approximate walking time from the visitor. The map opens on Universiti Malaya by default; set `ECOSWAP_MAP_CENTER="lat,lng"` and `ECOSWAP_MAP_ZOOM` to change it. New listings wait in **Admin → Pending listings**, where they can be approved, rejected, or sent back with a change request. Only approved listings appear in Explore. Editing the important details of an approved listing sends it back for review.

## 12. Exchange workflow

Request → owner accepts (both items are reserved; conflicting requests are closed) → students meet on campus → **both** confirm the handover → COMPLETED. Once a request is accepted, the two students can chat on the **Messages** page; they can also confirm the handover there. Completed exchanges, items reused and recorded weight on the homepage and Impact page update as soon as the second student confirms. For a swap, the requester offers one of their own approved items. Pending requests can be cancelled. Accepted exchanges can be withdrawn with a reason, which releases the items. See `documentation/USER_WORKFLOW.md`.

## 13. Image storage

Uploads go to `ECOSWAP_STORAGE_DIR` (default `./storage`, with folders `items/`, `profiles/`, `support/` and `temporary/`), outside the web root. Files get random names, and the type is checked from the file's content. MySQL stores only the storage key and metadata. Files are served only through `/media/**`, which checks who may see each one. The `StorageService` interface is ready for an S3 or Cloudflare R2 implementation.

## 14. E-mail configuration

Set `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` and `ECOSWAP_MAIL_FROM`. Without SMTP (local development), e-mails are written to the application log; copy verification and reset links from there. `ECOSWAP_REQUIRE_EMAIL_VERIFICATION=true` makes verification mandatory before approval.

## 15. Running the tests

```bash
./scripts/run-local.sh --test          # or: ./mvnw clean verify   (needs TEST_DB_* / DB_PASSWORD in the environment)
.\scripts\run-local.ps1 -Test
```

There are 35 tests: unit tests (JUnit 5 + Mockito) plus integration tests against the `ecoswap_test` MySQL database. The integration tests cover the approval lifecycle, security rules, and real concurrent acceptance races. Results are in `documentation/TEST_REPORT.md`.

## 16. Production deployment

Build with `./mvnw clean package`, then run `java -jar target/ecoswap-1.0.0.jar` with `SPRING_PROFILES_ACTIVE=prod` behind HTTPS. See `documentation/DEPLOYMENT_GUIDE.md` for the checklist (secure cookies, reverse proxy, backups, persistent storage, SMTP, monitoring).

---

### Project layout

```
src/main/java/com/ecoswap/      config · common · auth · user · category · item · exchange · favorite · notification
                                dashboard · approval · admin · report · support · audit · impact · storage · web
src/main/resources/             application*.yml · db/migration · templates/{layout,fragments,pages} · static/{css,js,img,fonts}
src/test/java/com/ecoswap/      unit + MySQL integration tests
documentation/                  docs, wireframes, screenshots
scripts/                        setup-database.sql · run-local.sh · run-local.ps1 · build-deliverables.py
storage/                        uploaded files (git-ignored)
```

`ECOSWAP_ALL_FILES.md` contains the complete source of every text file. `documentation/FILE_MANIFEST.md` lists every file and what it is for.
