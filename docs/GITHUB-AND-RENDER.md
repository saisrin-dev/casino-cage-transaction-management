# GitHub and live demo — Sai Srinath

These commands are for the **new** casino project; do not run them in the RetailFlow folder. The intended repository is `saisrin-dev/casino-cage-management`. It has not been created or pushed by this archive.

## 1. Validate locally

Run the README's Docker start. Sign in, create synthetic activity, verify CTR thresholds, switch to independent approver/filer accounts and reconcile a mock batch. Confirm that the UI/API work before publishing.

## 2. Create the GitHub repository

On https://github.com/new choose owner `saisrin-dev`, repository `casino-cage-management`. Choose your visibility. Leave README, license and gitignore initialization unchecked; the source already contains a README and gitignore. This avoids the unrelated-history merge encountered with RetailFlow.

```powershell
cd C:\Projects\casino-cage-management
git init -b main
git config user.name "Sai Srinath"
# This is the email you used in the latest successful portfolio commit:
git config user.email "nsaisrinath@gmail.com"
git add .
git status --short
git diff --cached --stat
```

Review the staged list. `.env`, database files, dependency folders and build outputs must be absent. Then:

```powershell
git commit -m "Build Casino Cage transaction and compliance demo"
git remote add origin https://github.com/saisrin-dev/casino-cage-management.git
git push -u origin main
```

If a remote already exists, inspect `git remote -v` before changing it. Do not force-push. Check GitHub Actions **Build and test** for actual CI results.

## 3. Provision persistent PostgreSQL

Unlike RetailFlow's resettable H2 demo, this project requires PostgreSQL. Choose a hosted PostgreSQL provider and plan that suits your budget; inspect the price/expiry/backup terms at signup. No paid plan is assumed or selected here.

Create the database in the same region as the Render app when supported. Convert the provider connection fields to JDBC:

```text
jdbc:postgresql://HOST:5432/DATABASE?sslmode=require
```

Use actual provider values for HOST and DATABASE. Set DB_USER and DB_PASSWORD separately; do not put secrets in source code or screenshots. Internal Render database endpoints can use their recommended TLS setting; follow the provider's connection instructions. Never use a service's `postgres://...` URL directly as DB_URL without JDBC conversion.

## 4. Render web service

Connect the new GitHub repository as you did with RetailFlow. Render fields:

| Field | Value |
| --- | --- |
| Name | `casino-cage-management` (availability determines final URL) |
| Language | Docker |
| Branch | main |
| Root Directory | Empty |
| Docker build context | `.` |
| Dockerfile | `./Dockerfile` |
| Docker Command | Empty; Dockerfile supplies entrypoint |
| Health Check Path | `/actuator/health` |
| Compute | Select a plan only after reviewing its current cost and memory |
| Auto deploy | After CI checks pass if available, otherwise manual/on commit as chosen |

Environment variables:

```text
DB_URL=jdbc:postgresql://YOUR_HOST:5432/YOUR_DATABASE?sslmode=require
DB_USER=YOUR_DATABASE_USER
DB_PASSWORD=YOUR_DATABASE_PASSWORD
DATA_ENCRYPTION_KEY=YOUR_GENERATED_32_BYTE_BASE64_KEY
DEMO_PASSWORD=YOUR_SEPARATE_PUBLIC_DEMO_PASSWORD
DEMO_SEED=true
SIMULATION_CONTROLS=true
FILING_MODE=mock
COOKIE_SECURE=true
KAFKA_ENABLED=false
```

`PORT` is supplied by Render and honored by Spring Boot. The container serves Angular and REST from one origin. Do not deploy the event-monitor's unauthenticated local HTTP port as a public service. Kafka is optional for the hosted core demo; run the full architecture locally or configure a secured external broker separately.

Generate a different encryption key for each environment and preserve it while the database exists. Public demo users share a configured password intentionally; only synthetic data belongs in this environment. Never use your GitHub password as DEMO_PASSWORD.

## 5. Verify the live URL

Check deployment status, then `/actuator/health`, the login page, role navigation, patron CRUD, a >$10,000 cash-in event, independent approval, simulated submission and acknowledgment, tax/W-9 separation, and persisted data after app restart. A healthy HTTP service alone does not verify all workflows.

Once live, use the actual Render URL for the portfolio's Live Demo button and https://github.com/saisrin-dev/casino-cage-management for Source Code. Add a short visible demo notice and the public demo password only if you intentionally want visitors to explore shared synthetic accounts. Keep database secrets private.

## References

- https://render.com/docs/docker
- https://render.com/docs/web-services
- https://render.com/docs/health-checks
- https://render.com/docs/postgresql-creating-connecting
- https://angular.dev/reference/versions

The complete Docker Compose stack has not been executed in this build environment, which does not expose a Docker daemon. Local PostgreSQL/JVM and UI checks are reported separately in VERIFICATION.md.
