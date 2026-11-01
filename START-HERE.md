# Start here — Sai Srinath

This folder is the source for your new **CageLedger / Casino Cage Management** portfolio project. It has not yet been pushed to GitHub or deployed.

1. Extract it as `C:\Projects\casino-cage-management`.
2. Install/start Docker Desktop if it is not already running. Use Linux containers.
3. Open PowerShell in this folder and run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\configure.ps1
docker compose up --build
```

4. Open http://localhost:8080. Choose a demo role. Find your generated login password in the `DEMO_PASSWORD` line of `.env`.
5. Follow `docs/DEMO-WALKTHROUGH.md` to explore the application.
6. Follow `docs/GITHUB-AND-RENDER.md` when ready to publish.

The core stack is Java 17 / Spring Boot / Angular / PostgreSQL. Optional Kafka and Node.js services, a separate C++ module, and a Java Swing client match the technology themes in your uploaded Everi resume. They are separate examples and do not claim to reproduce employer code or scale.

Use synthetic data only. All filings are simulations. Tax calculations are visibly limited demonstration rules. Read `docs/REQUIREMENTS-COVERAGE.md` for implemented and remaining scope, and `docs/VERIFICATION.md` for actual test results.
