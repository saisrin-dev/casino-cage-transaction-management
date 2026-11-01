# CageLedger — Open Casino Cage Demo

The default deployment now opens without login or an external database. It runs Angular with browser-local synthetic data, hosted by a lightweight Node server. See [UPDATE-DEMO.md](UPDATE-DEMO.md) for installation, Render deployment, transaction entry, and limitations.

**Public sandbox only:** no authenticated permissions, real patron data, real filings, or payments. Each browser has separate records. Clearing browser storage or resetting the demo restores sample data. The Java/Spring Boot/PostgreSQL implementation remains below as the original backend reference; its setup instructions do not apply to the default browser demo.

---

# CageLedger — Casino Cage & Compliance

An independent portfolio application by **Sai Srinath**. Patron and cage transaction management, CTR review, restricted SAR work queues, demonstration tax forms, and deterministic filing simulation.

**Synthetic data only. No payments, bank transfers, credit authorizations, FinCEN filings, or IRS submissions occur.** This is a working portfolio release, not an approved casino production platform. See [coverage and limitations](docs/REQUIREMENTS-COVERAGE.md) before evaluating scope. All current screens connect to the server; PostgreSQL is authoritative, not browser local storage.

## Technology choices

The user's later instruction to match the uploaded Everi resume overrides the React/Vite/Material UI frontend in the original requirements.

| Component | Technology | Purpose |
| --- | --- | --- |
| Main API | Java 17, Spring Boot 3.5.16, Spring MVC/Security/Data JPA | Validated transactions, role authorization, report workflows |
| Main UI | Angular 20.3.34, TypeScript 5.9.2, RxJS 7.8.2 | Responsive six-module operations workspace |
| Database | PostgreSQL 16, Flyway | Persistent financial records, immutable audit trigger, migrations |
| Events | Spring Kafka, transactional outbox | At-least-once delivery of committed transaction events |
| Event monitor | Node.js 22, KafkaJS 2.2.4 | Validates, deduplicates and persists Kafka events; JSON and SSE endpoints |
| Native example | C++17, standard threads, CMake | Integer-cent validation and concurrent test harness |
| Legacy client | Java Swing + Java HttpClient | Login and read patron/transaction data from the same secured API |
| Verification | JUnit 5, Mockito, MockMvc, PostgreSQL, Playwright, Node test runner | Business, permissions, concurrency and browser workflows |
| Delivery | Docker Compose, GitHub Actions, multi-stage Dockerfile | Local stack and Render-compatible web container |

The C++ executable and Swing client are separate examples, not dependencies of transaction posting. They do not establish or reproduce employment metrics. PostgreSQL was retained because no database was named in the Everi role of the uploaded resume. Oracle, Kubernetes, AWS and production Kafka capacity are not claimed for this release.

## Quick start — Windows PowerShell

Prerequisites: Docker Desktop running with Linux containers. The Docker route does not require Maven or Node on the host. For individual development: JDK 17, Maven 3.9+, Node 22.12+ (your Node 22.23.4 is compatible), and PostgreSQL 16. Java/Swing requires a graphical desktop; C++ requires a compiler.

Extract the ZIP so `Dockerfile` and `compose.yml` are here:

```powershell
cd C:\Projects\casino-cage-management
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\configure.ps1
docker compose up --build
```

Open **http://localhost:8080**. Read `DEMO_PASSWORD` from your local `.env`; the script generates it. Do not share the complete `.env` or encryption key. This configuration is for synthetic local demonstrations.

```powershell
# Show only your local demo login password when you need it:
Select-String -Path .env -Pattern '^DEMO_PASSWORD='
```

Mac/Linux:

```sh
sh scripts/configure.sh
docker compose up --build
```

Stop with Ctrl+C, or `docker compose down`. Database records persist in the `cage-db` Docker volume. `docker compose down -v` destroys this demo data. **Preserve the same DATA_ENCRYPTION_KEY** when restarting or restoring a database. The seed runs only when enabled and the user table is empty; changing DEMO_PASSWORD later does not reset existing user password hashes.

## Accounts and walkthrough

All seeded accounts use the generated `DEMO_PASSWORD`.

| Username | Role | Suggested demonstration |
| --- | --- | --- |
| `cashier` | Cage Cashier | Create patron, enter draft/post transaction, download synthetic receipt |
| `supervisor` | Cage Supervisor | Correct/void activity with a reason, merge duplicate profiles, temporary identities |
| `analyst` | Compliance Analyst | Review CTRs, create synthetic SARs, attach evidence, send for review |
| `approver` | Compliance Approver + Tax Specialist | Independently approve reports; activate proposed transaction types |
| `filer` | Authorized Filer | Select approved revisions, prepare/export/mock submit, reconcile item outcomes |
| `tax` | Tax Specialist | Edit eligible demo tax forms and W-9 lifecycle |
| `admin` | Administrator | Property metadata, configuration proposals, user creation, transaction type proposals |
| `auditor` | Read Only Auditor | Read permitted patron/transaction/CTR records and their history |

Administrator **does not** imply SAR permission. Approver's additional TAX role is an explicit demo assignment. It can review tax reports created by a different user. Default creator/approver separation is enforced by the API. User-created accounts accept one or more valid comma-separated roles through the API; UI selects one.

1. **Subject Manager:** search by name, ID, loyalty, date of birth or masked values; open profile, edit with reason, inspect history and linked activity. Exact duplicate names require supervised review. Only synthetic TIN/ID values starting `DEMO-` are accepted. Supervisor merges preserve the original profile and recalculate affected CTR groups.
2. **Transaction Manager:** select beneficiary, approved type, actual cash/gross, event timestamp, reference and source ID. Optional conductor is stored separately. Save draft or post. Source IDs enforce retry protection. Posted corrections/voids require supervisor permission and retain originals. Draft editing creates a linked replacement rather than deleting evidence.
3. **CTR Manager:** Review, Batch and History tabs. Review directional totals, excluded/reversed source activity, validations and revisions. Send for review as analyst, approve as approver. Filer checks approved rows, prepares a batch, exports optional mock JSON, submits to simulator, then explicitly reconciles. Submitted and accepted are separate facts.
4. **SAR Manager:** restricted Review/Batch/History; manual creation with synthetic narrative, detection date, activity range and category. Link additional subjects and evidence. Demo repeated-cash-in referral creates a restricted analyst task, not an automatic SAR or finding of wrongdoing.
5. **Tax Forms Manager:** separate Review/Batch/History and W-9 Register. Demo W-2G and promotional 1099-MISC calculations; 1098 is rejected. Nonresident cases stop for specialist review. Source-linked amounts must reconcile with the posted transaction. W-9 never enters report batches.
6. **Setup Manager:** edit property name/contact; clone/propose types as administrator, switch to approver and open Transaction Manager's pending type approvals, then activate independently. Deactivate as admin; historical entries retain labels/mappings. Other policy JSON edits are **proposals only** and create a review task. They do not alter statutory rules. Gaming-day rules are immutable in this release to avoid silently reclassifying old activity.

## Business rules implemented

- Reportable currency in/out are derived from the approved transaction definition and the actual cash component. Noncash cannot contain currency. Slot jackpots are CTR-excluded; later separate movements are separate events. Exchange/device demo mappings are explicit examples, not a complete legal exceptions engine.
- Cash-in and cash-out are aggregated independently by beneficiary and property gaming day. Each direction must be **greater than** $10,000 to generate a CTR candidate. Exactly $10,000 does not qualify. $7,000 in plus $6,000 out is not a trigger. Amounts use BigDecimal and fixed database precision.
- Gaming day defaults to 06:00 America/New_York. Instant-based local boundary comparison is tested at spring/fall DST changes. Ambiguous boundaries choose the earlier offset; nonexistent boundaries resolve forward, matching Java ZonedDateTime defaults.
- A property row lock serializes posting, correction, void, and batch validation. This intentionally trades throughput for demo correctness. Unfiled activity updates one candidate; filed activity opens an impact task without overwriting its snapshot.
- A posted correction retains the original as CORRECTED and links the new transaction. Voids preserve the original. Gross and cash are positive/nonnegative, cash cannot exceed gross, and only USD is supported in this release.
- Material report/source/subject/evidence changes revoke approval. Batch transmission revalidates the exact approved revision. Batch membership is unique per report/revision.
- CTR due date is gaming day + 15 calendar days. SAR default is detection + 30 calendar days. Extended 60-day unidentified-subject SAR workflow is not implemented. Tax recipient due date displayed in seed is an illustrative date, not a complete adjusted deadline calendar.
- Internal CTR rejection preserves an outstanding obligation task. Rejected external items need a new linked correction report. Filed snapshots remain separate from the current patron profile.
- Tax rules are limited, versioned `DEMO-2026-v1` fixtures. They are not a complete IRS table, state rules, backup withholding engine or foreign-payee determination engine. No tax rules are presented as production advice.

## Filing simulator

`FILING_MODE=mock` always labels files and IDs as simulated. The export schema `CAGE-MOCK-1` is a project JSON format, **not a FinCEN or IRS filing format**. Production adapters throw an error without making a network call.

| Scenario | Submission | Reconciliation |
| --- | --- | --- |
| ACCEPT | Awaiting response | Every item accepted |
| WARN | Awaiting response | Accepted with synthetic warning |
| REJECT | Awaiting response | Every item rejected with MOCK-E101 |
| PARTIAL | Awaiting response | Alternating accepted/rejected items |
| DELAY | Awaiting response | No result before 15 seconds, then accepted |
| TIMEOUT | Transport failed, outcome unknown | Mock transport lookup reconciles accepted results; resubmit blocked |

Nondefault scenarios require `SIMULATION_CONTROLS=true`. The production adapter boundary is intentionally unimplemented and fails closed. No public regulatory API is assumed. Mock acknowledgments have `MOCK-` IDs; they are not BSA IDs. Raw response, item errors and export checksum are preserved.

## Individual backend / frontend startup

Start the database using `docker compose up -d db`. Load your `.env` into the process environment. In PowerShell:

```powershell
Get-Content .env | ForEach-Object {
  if ($_ -match '^([A-Z_]+)=(.*)$') {
    [Environment]::SetEnvironmentVariable($matches[1], $matches[2], 'Process')
  }
}
$env:DB_URL = 'jdbc:postgresql://localhost:5432/cage'
$env:DB_USER = 'cage'
mvn -f backend/pom.xml spring-boot:run
```

In a second PowerShell:

```powershell
cd C:\Projects\casino-cage-management\frontend
npm.cmd ci
npm.cmd start
```

Open http://localhost:4200. Angular proxies `/api` to :8080. Authentication uses a server session and CSRF token, not a token in browser local storage. To serve the built UI in Spring Boot outside Docker:

```powershell
npm.cmd run build
New-Item -ItemType Directory -Force ..\backend\src\main\resources\static
Copy-Item -Recurse -Force .\dist\cage\browser\* ..\backend\src\main\resources\static\
```

## Verification

Use a separate empty PostgreSQL database, **not the demo database**, for integration tests. The tests enable seed data; their transactions roll back except the explicit concurrency fixture. CI creates `cage_test` automatically.

```powershell
# With PostgreSQL running via Compose:
docker compose exec db createdb -U cage cage_test
$env:DB_URL = 'jdbc:postgresql://localhost:5432/cage_test'
# DB_USER, DB_PASSWORD and DATA_ENCRYPTION_KEY must be loaded as above.
mvn -f backend/pom.xml verify
cd frontend
npm.cmd ci
npm.cmd run build
npx.cmd playwright install chromium
# Start the application with DEMO_PASSWORD=Integration-Test-2026 in a separate
# terminal against a demo database; then:
npm.cmd run test:e2e
```

The seeded test accounts in `cage_test` use `Integration-Test-2026`. Reusing that database with a different DEMO_PASSWORD does not reset them. Read [verification results](docs/VERIFICATION.md) for exactly what was run here. Included browser tests require the running app; a unit-test pass is not a browser pass.

```sh
cd event-monitor && npm ci && npm test
cmake -S native-validator -B native-validator/build
cmake --build native-validator/build
ctest --test-dir native-validator/build --output-on-failure
javac -d desktop-client/build desktop-client/CageDesktop.java
java -cp desktop-client/build CageDesktop
```

On Windows, CMake can generate a Visual Studio 2022 C++ solution: `cmake -S native-validator -B native-validator/build -G "Visual Studio 17 2022"`, then `cmake --build native-validator/build --config Release` and `ctest --test-dir native-validator/build -C Release`.

## Optional Kafka + Node.js demonstration

The main web demo works without Kafka; committed events accumulate in the outbox. To run the local event architecture:

```powershell
$env:KAFKA_ENABLED = 'true'
docker compose --profile events up --build
```

`GET http://localhost:3001/events` returns the most recent 100 synthetic transaction events. `/stream` is Server-Sent Events and `/health` reports broker connectivity. The monitor is bound to localhost by Compose and is not intended for public hosting. It stores an append-only NDJSON log in its volume and deduplicates event IDs across restarts. It is not a downstream payment processor. The outbox can redeliver after crashes; consumers deduplicate. No exactly-once or measured scale claim is made.

## Environment variables

| Variable | Meaning / default |
| --- | --- |
| DB_URL | JDBC PostgreSQL URL; default localhost:5432/cage |
| DB_USER / DB_PASSWORD | Database credentials; password required |
| DATA_ENCRYPTION_KEY | Required base64 of 32 random bytes; AES-256-GCM data encryption key |
| DEMO_SEED | Default false; enable only for synthetic demo initialization |
| DEMO_PASSWORD | At least 12 characters when seeding; no default shared production password |
| FILING_MODE | `mock`; any other value selects failing production adapter |
| SIMULATION_CONTROLS | Default false; permits controlled mock response scenarios |
| PORT | HTTP port; default 8080; Render supplies its own |
| COOKIE_SECURE | False locally; set true on HTTPS deployment |
| KAFKA_ENABLED / KAFKA_BOOTSTRAP | Optional publisher; false / localhost:9092 |
| JAVA_TOOL_OPTIONS | JVM memory options; container sets conservative percentage |

## Security / operations

BCrypt passwords; Spring session authentication; HTTP-only SameSite=Strict session cookie; CSRF on mutations; permission checks on API and evidence downloads; no default SAR privileges for admins. TIN/ID, report narrative/fields/snapshots, evidence and audit before/after payloads are AES-GCM encrypted in PostgreSQL. The application masks sensitive profile fields. Evidence is limited to 1 MiB per file, downloaded as attachment and is for synthetic documents only; antivirus scanning is not implemented.

The database rejects audit update/delete with a trigger. This is application-level audit protection, **not** a defense against a database superuser. Retention schedules are documented/configuration proposals; automated retention, legal holds and production key management remain future work. For backups use `sh scripts/backup.sh`; restore into an empty database with `pg_restore --no-owner --no-privileges -U cage -d cage backup.dump`. Preserve encryption key separately. Backup/restore has not been operationally certified.

Public demo: never enter real patrons, TINs, confidential SAR narratives, or real evidence. Keep the demo in its own database. Shared demo users intentionally enable role switching for portfolio evaluation; this does not represent a production identity-management design.

## Publish and package

Follow [GitHub and Render instructions](docs/GITHUB-AND-RENDER.md) after local verification. The Render route needs an external persistent PostgreSQL database; there is no in-memory fallback. Kafka and companion clients are not required for the small hosted web demonstration.

```powershell
python .\scripts\package.py
```

The archive excludes dependencies, build output, `.env`, local data and Git history. See [file tree](docs/FILE-TREE.md), [API guide](docs/API.md), [schema](docs/SCHEMA.md) and [architecture](docs/ARCHITECTURE.md).
