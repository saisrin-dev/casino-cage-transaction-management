# Install the open browser demo

This update replaces the default Render runtime with a lightweight Node static server and an Angular browser-local simulator. It requires no login, PostgreSQL, encryption key, or demo password. Existing Java/Spring Boot source remains in backend/ for reference; Dockerfile.backend preserves the prior image definition. The public demo does not run that backend.

## Apply on Windows

1. Extract this ZIP into `C:\Projects\casino-cage-transaction-management`, replacing matching files. The ZIP contains paths relative to that repository root, not an extra project directory.
2. In PowerShell:

```powershell
cd C:\Projects\casino-cage-transaction-management
git add Dockerfile Dockerfile.backend demo-server.mjs package.json README.md START-HERE.md UPDATE-DEMO.md frontend/src/main.ts frontend/src/app.html frontend/src/styles.css frontend/src/demo-store.ts frontend/test/demo-store.test.mjs frontend/e2e/browser-demo.spec.ts frontend/playwright.config.ts frontend/angular.json .github/workflows/ci.yml
git commit -m "Add open browser demo, refreshed screens and transaction entry"
git push
```

3. Render auto-deploys the pushed commit. Keep Docker runtime, Oregon, root blank, Free compute, Dockerfile `./Dockerfile`, build context `.`, command blank, and health check `/actuator/health/readiness`.
4. Once this new deployment is Live, open the demo: it should open directly to the dashboard. Add a transaction from **New transaction** or **Transaction Entry**. Reload and verify the record remains in Transaction Manager.
5. Remove the old DB_URL, DB_USER, DB_PASSWORD, DATA_ENCRYPTION_KEY, DEMO_PASSWORD, DEMO_SEED, SIMULATION_CONTROLS, FILING_MODE, COOKIE_SECURE and KAFKA_ENABLED environment variables from this web service. The new runtime does not read them. This is optional for functionality but keeps service configuration clear.
6. The old `casino-cage-db` is no longer used by this demo. Leave it until you have confirmed the new version works. Deleting it is a separate optional step; any original server-side records will not be migrated.

## What changes

- Direct access to all demo screens with no accounts or role switching.
- Dedicated responsive transaction-entry page: patron, type, gross/cash, station, reference, event timestamp, draft/post, notes/reason, live preview and saved-record receipt.
- Navy sidebar, indigo calls to action, visual dashboard, status cards, improved tables, drawers and forms.
- Browser storage for synthetic patrons, transactions, reports, batches, attachments and audit history. Each browser has its own independent demo. Reset in the sidebar or Setup restores sample data.
- Transactions use integer cents for validation and directional aggregation, a New York 06:00 gaming cutoff, duplicate source checks, draft/post/void/correction, and simulated CTR generation above $10,000 in either direction.
- Review/approval and mock filing are educational state transitions. There is no authenticated access control or independent-approver enforcement in this public browser demo. No regulator or payment network is contacted. SAR and tax data must remain synthetic.
- Browser clearing/reset loses demo changes. No shared cross-device data, server backup, or migration from PostgreSQL. Storage failures show an error and roll back the attempted change. If browser storage is blocked, a notice explains that changes last for the current visit only.
- Optional evidence stays in browser storage, up to 1 MiB per file; total storage depends on browser quotas.

## Local validation

With Node 22.18+ or 24:

```powershell
cd frontend
npm ci
npm run build
cd ..
npm test
node demo-server.mjs
```

Open http://localhost:8080. No Docker Desktop or local database is required. Stop with Ctrl+C. The default GitHub Actions workflow also retains Java/backend, Node monitor, C++ and desktop compile checks and now targets browser-demo.spec.ts for the new public UI. The original authenticated workflow tests remain in workflows.spec.ts as backend-era reference tests; they are not the default public-demo smoke suite.
