# Installation Guide

This guide expands on README sections 1–8, with commands for both platforms and fixes for common problems.

## Prerequisites

| Tool | Version | Check |
|---|---|---|
| JDK | 21 | `java -version` |
| MySQL Server | 8.0.x | `mysql --version` |
| Maven | via the wrapper (3.9.11) | `./mvnw -v` |
| Git | any | `git --version` |

## Step by step — Linux / macOS

```bash
git clone <repository-url> EcoSwap && cd EcoSwap
sed -i.bak "s/CHANGE_ME/$(openssl rand -base64 18)/" scripts/setup-database.sql   # or edit by hand
mysql -u root -p < scripts/setup-database.sql
cp .env.example .env
#   DB_PASSWORD=<the password you put in setup-database.sql>
#   TEST_DB_PASSWORD=<same>
#   ECOSWAP_ADMIN_EMAIL=you@university.edu
#   ECOSWAP_ADMIN_PASSWORD=<8+ chars, letter + number>
#   ECOSWAP_REMEMBER_ME_KEY=$(openssl rand -hex 32)
./scripts/run-local.sh
```

## Step by step — Windows PowerShell

```powershell
git clone <repository-url> EcoSwap; Set-Location EcoSwap
notepad scripts\setup-database.sql          # replace CHANGE_ME
Get-Content scripts\setup-database.sql | mysql -u root -p
Copy-Item .env.example .env; notepad .env    # fill in the values listed above
.\scripts\run-local.ps1
```

If PowerShell blocks the script: `Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass`.

## First run checklist

1. The log shows `Successfully applied 15 migrations to schema ecoswap_db`.
2. The log shows `Created the initial administrator account …`.
3. <http://localhost:8080> shows the homepage, with all statistics at 0 on a new install.
4. Log in at `/login` with the admin credentials; you should land on `/admin`.
5. Register a test student in a private window, then approve it in **Pending registrations**.

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `Access denied for user 'ecoswap'@'localhost'` | `DB_PASSWORD` doesn't match the password in `setup-database.sql` |
| `Public Key Retrieval is not allowed` | Keep `allowPublicKeyRetrieval=true` in `DB_URL` for local MySQL 8 |
| `Schema-validation: missing column` | The database was changed by hand. Never edit applied migrations; add `V16__…sql` instead |
| `No administrator exists` warning | Set `ECOSWAP_ADMIN_EMAIL` and `ECOSWAP_ADMIN_PASSWORD`, then restart |
| "Remember me" stops working after a restart | Set a fixed `ECOSWAP_REMEMBER_ME_KEY` |
| No verification e-mail arrives | SMTP isn't configured, so the link is in the application log (`[mail disabled]`) |
| Uploads fail with "file storage" errors | Make sure `ECOSWAP_STORAGE_DIR` exists and is writable by the app's user |
| Port 8080 already in use | Set `PORT=8081` in `.env` |
