# Verification results

Verified October 31, 2026 (America/New_York). Results below are actual executed checks; future hosted CI/deployment results must be checked separately.

| Check | Result |
| --- | --- |
| Java backend unit/rule tests | 18 passed |
| PostgreSQL 16.15 workflow integration tests | 35 passed, including two concurrent posting transactions |
| Mockito outbox/adapter tests | 3 passed |
| Total backend | **56 tests, 0 failures, 0 errors, 0 skipped** |
| Angular production build | Passed; approximately 291 kB initial raw bundle at final functional build |
| Playwright browser smoke/E2E | **6 passed** against running Spring Boot + PostgreSQL |
| Node.js event monitor tests | **3 passed** |
| C++17 | g++ compilation passed; validation boundaries and 100 concurrent validation jobs passed |
| Swing | Java 17 compilation passed; desktop UI was not interactively exercised |
| OpenAPI | Authenticated endpoint returned 200; 32 paths exported; standalone JSON includes examples and session/CSRF notes |
| YAML | Application, Compose and GitHub Actions YAML parsed |
| npm production dependency audit | UI and event monitor each reported **0 vulnerabilities** at verification time; this is not an all-dependencies or future guarantee |
| Visual inspection | Desktop dashboard screenshot inspected; mobile navigation tested at 390×844 |

Browser scenarios: cashier transaction entry and receipt availability; SAR review/audit; administrator role restrictions and type cloning; separate W-9 lifecycle; mobile navigation; analyst-to-approver-to-filer CTR flow through simulated acknowledgment. All six passed in 8.3 seconds on the verification run.

Environment: OpenJDK 17.0.20, Maven 3.9.11, PostgreSQL 16.15, Node 24.22.1, Linux. Runtime deployment configuration targets Java 17 and Node 22. Browser automation used the available Chromium 134 headless shell with the pinned Playwright runner; the runner's newest bundled-browser download was unavailable in this environment. No claim is made about exhaustive cross-browser coverage.

Database runs used fresh isolated PostgreSQL databases with Flyway migrations, encrypted columns, and synthetic seeds. Production-like data volumes were not used. SQL failure tests confirm the audit trigger rejects mutation. The PostgreSQL checks are not H2 substitutions.

Not executed here: the complete Docker Compose stack, a live Kafka broker/Node integration run, Docker image build, GitHub Actions on GitHub, hosted Render deployment, backup/restore drill, production integrations, load/performance targets, comprehensive security/accessibility audit. Docker daemon access was unavailable. Broker acknowledgment and failure handling were covered with Mockito; Node replay/persistence were tested separately. See REQUIREMENTS-COVERAGE.md for functional constraints and remaining scope.

Formatting after functional checks does not intentionally change behavior. Build outputs, downloaded dependencies, screenshots, local credentials, runtime databases and test traces are excluded from the distributable archive.
