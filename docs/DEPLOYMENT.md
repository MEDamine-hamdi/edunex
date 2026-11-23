# EduNex / TekMS: deployment and operations runbook

## How the pieces fit

Browser or phone -> **Vercel** (Next.js web app; it forwards `/api` to the backend) -> **Coolify on the OVH VPS** (Spring Boot, port 8099) -> **Neon** (PostgreSQL).
E-mails go through **Brevo**, the AI through **Groq**. Uploaded files live on the VPS in `/data/storage` (a Coolify persistent volume).

The browser only talks to the Vercel address, so the session cookie belongs to that address and is never sent to another site.

## Backend settings (Coolify -> application -> Environment Variables)

Any property of `application.properties` can also be set as an environment variable: upper case, dots replaced by underscores.

| Variable | What it is |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Neon connection (`jdbc:postgresql://...?sslmode=require`) |
| `JWT_SECRET` | 64 random bytes in Base64. New value = everybody signs in again |
| `JWT_EXPIRATION_MS` | Session length. `28800000` = 8 hours |
| `APP_PUBLIC_URL` | Address of the **web app** (https, no trailing slash). Used in e-mail links |
| `APP_COOKIE_SECURE` | `true` in production (https) |
| `APP_SECURITY_STRICT` | `true` refuses to start when the JWT secret is weak |
| `APP_TIMEZONE` | For example `Africa/Tunis` |
| `GROQ_API_KEY` | AI assistant, timetable import, quizzes |
| `BREVO_API_KEY`, `MAIL_SENDER_EMAIL`, `MAIL_SENDER_NAME` | E-mails. The sender domain must be authenticated in Brevo (SPF and DKIM) |
| `SPRINGDOC_ENABLED` | `false` in production (hides the API documentation) |
| `APP_STORAGE_PATH` | `/data/storage` (the persistent volume) |
| `DB_POOL_SIZE` | Database connections, default 20 |
| `JAVA_TOOL_OPTIONS` | For example `-XX:MaxRAMPercentage=65 -XX:+ExitOnOutOfMemoryError` |

Web app (Vercel): `BACKEND_URL` = backend address (https, no trailing slash, no `/api`). It is read at build time: **redeploy without the build cache** after changing it.

## First deployment checklist

1. Backend: set the variables above, **Build Pack = Dockerfile**, base directory `/backend`, port `8099`, add the persistent volume `/data/storage`, attach the domain.
2. Check `https://<backend>/api/auth/health` answers `{"status":"ok"}`.
3. Vercel: set `BACKEND_URL`, deploy. Check `https://<web>/api/auth/health` gives the same answer.
4. Sign in as the platform administrator, create the first university and its administrators (they receive their credentials by e-mail).
5. **Before going live:** change the platform administrator password, delete the test accounts (`etu1`, `prof1`, `agent1`...), and rotate any secret that was ever shared in clear text (database password, JWT secret, Groq and Brevo keys).

## Updating

`git push origin main`, then **Deploy** (not Restart) the backend in Coolify and redeploy the web app on Vercel. The database changes itself on start (new tables and columns are added, nothing is dropped). Running requests finish before the old version stops.

## Backups

* Database: Neon keeps its own history. Check the retention of your plan.
* **Uploaded files are NOT backed up by Neon.** Use an OVH snapshot of the VPS or a scheduled copy of `/data/storage`, and test a restore once.

## When something breaks

| Symptom | Most likely cause |
|---|---|
| Login says "Une erreur est survenue (code 502/503/504)" | Backend not running. Coolify -> Logs. With `APP_SECURITY_STRICT=true` a weak `JWT_SECRET` stops the start |
| Code 404 on login | `BACKEND_URL` missing or wrong on Vercel (redeploy without cache) |
| Reset-password e-mails point to `localhost` | `APP_PUBLIC_URL` not set on the backend |
| Account creation says "E-mail service is not configured" | `BREVO_API_KEY` / `MAIL_SENDER_EMAIL` missing: credentials cannot be sent, so the account is not created |
| Uploaded files vanish after a deploy | The persistent volume is missing |
| Site freezes under load | Database pool exhausted: raise `DB_POOL_SIZE`, check the Neon connection limit |

## Good to know

* Student numbers, employee numbers, subject codes and class names are unique across the whole platform, not per university. Use a prefix per university (for example `USFAX-2026-001`).
* Messaging refreshes by polling (every 2 seconds in an open conversation), so it works behind any proxy.
* Each university only sees its own data; the platform administrator sees everything.