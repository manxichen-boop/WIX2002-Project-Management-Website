# Deployment Guide

## Build

```bash
./mvnw clean verify          # tests against the ecoswap_test database
./mvnw -DskipTests package   # produces target/ecoswap-1.0.0.jar (self-contained)
```

## Runtime configuration (production)

| Variable | Value |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` (cached templates, 7-day static caching, secure cookies, proxy-aware) |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Managed MySQL 8 with TLS (`useSSL=true`, no `allowPublicKeyRetrieval`) |
| `ECOSWAP_BASE_URL` | Public HTTPS URL, used in e-mailed links |
| `ECOSWAP_STORAGE_DIR` | A persistent volume, backed up, **not** inside the deployed jar's folder |
| `ECOSWAP_REMEMBER_ME_KEY` | 64+ random characters, kept in a secret store |
| `ECOSWAP_ADMIN_EMAIL` / `ECOSWAP_ADMIN_PASSWORD` | First deploy only; remove the password afterwards |
| `ECOSWAP_ALLOWED_EMAIL_DOMAINS` | Your university's domains |
| `ECOSWAP_REQUIRE_EMAIL_VERIFICATION` | `true` |
| `SPRING_MAIL_*`, `ECOSWAP_MAIL_FROM` | Institutional SMTP / transactional e-mail service |

## Example: Linux VM with systemd + Nginx

```ini
# /etc/systemd/system/ecoswap.service
[Unit]
Description=EcoSwap
After=network.target
[Service]
User=ecoswap
EnvironmentFile=/etc/ecoswap/ecoswap.env      # chmod 600, owned by root
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar /opt/ecoswap/ecoswap-1.0.0.jar
Restart=on-failure
[Install]
WantedBy=multi-user.target
```

```nginx
server {
    listen 443 ssl http2;
    server_name ecoswape.example.edu;
    client_max_body_size 32m;                  # matches spring.servlet.multipart.max-request-size
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

Add `Strict-Transport-Security` at the proxy. The application already sends CSP, `X-Frame-Options: DENY`, `nosniff` and `Referrer-Policy`.

## Database

- Flyway migrates on startup. Give the application user DDL rights only during deploys if your policy requires it; otherwise run `flyway migrate` from CI with a migration user.
- Take daily `mysqldump --single-transaction ecoswap_db` backups along with the storage volume, and test restores.
- `spring.flyway.clean-disabled=true` protects production from accidental wipes.

## Scaling notes

- Sessions are stored in memory. For more than one instance, add Spring Session (JDBC or Redis) or use sticky sessions.
- Login throttling is in memory and per instance. Move it to a shared store for multi-instance deployments.
- File storage: implement `StorageService` for S3 or Cloudflare R2, select it in `StorageConfig`, and set the bucket credentials as environment variables. Keys and the database schema stay the same.

## Monitoring

Watch the application log for `ERROR` lines and `Concurrent update conflict` info lines (these are expected to be rare). Track the database size and the storage volume's free space.

## Free public hosting: Render (app) + Aiven (MySQL)

1. **Aiven** (https://aiven.io, free plan, no card): create a *MySQL* service on the **Free** plan. From its Overview page note Host, Port, User (`avnadmin`), Password and Database (`defaultdb`).
2. **Render** (https://render.com, sign in with GitHub): **New → Blueprint** → choose this repository. `render.yaml` creates a free Docker web service and asks for:
   - `DB_URL` = `jdbc:mysql://HOST:PORT/defaultdb?sslMode=REQUIRED&serverTimezone=UTC&characterEncoding=utf8`
   - `DB_USERNAME` = `avnadmin`, `DB_PASSWORD` = the Aiven password
   - `ECOSWAP_ADMIN_EMAIL` / `ECOSWAP_ADMIN_PASSWORD` = the first administrator (8+ characters, a letter and a digit)
3. The first build takes about 5–8 minutes. The site is then live at `https://<name>.onrender.com`.

Free-plan notes: the service sleeps after 15 minutes without visitors (the next visit takes about a minute to wake it), photos are stored in MySQL (`ECOSWAP_STORAGE_TYPE=database`) because the free disk is wiped on restart, and e-mail sending is off unless `SPRING_MAIL_*` is set (verification links are then only written to the log, so keep `ECOSWAP_REQUIRE_EMAIL_VERIFICATION=false`).
