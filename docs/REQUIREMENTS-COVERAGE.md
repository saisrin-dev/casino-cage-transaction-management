# Requirements coverage — portfolio release

The source specification is retained in ORIGINAL-REQUIREMENTS.md. The later instruction to match the uploaded Everi resume changed the main frontend to Angular/TypeScript and added separate Kafka/Node, C++ and Swing examples. This document records remaining scope explicitly. The archive is a working portfolio implementation; it does **not** claim every production requirement has been completed.

## Functional coverage

| Area | Implemented in this release | Remaining or deliberately constrained |
| --- | --- | --- |
| Subject Manager | Create/search/edit, masked synthetic identifiers, exact-name duplicate detection, supervisor merge, supervised temporary identity, linked transactions, field before/after audit | Multiple identity records per patron, configurable fuzzy matching/survivorship, real PII onboarding, full related-report UI |
| Transaction Manager | 21 starter definitions, patron/conductor linkage, gross/cash split, server-derived directional treatment, draft/post/correct/void, source retry protection, recorded gaming rules, history, receipt download | Arbitrary mixed-tender legs, FX, external import connectors, actual payment/marker credit execution, formal supervisor request/approval queue separate from supervisor action |
| Type configuration | Propose/clone, independent activate, deactivate, historical copied label/treatment/version | Full effective-date reclassification engine, flexible field/tender schema, historical type-version entities beyond recorded values |
| Gaming day / CTR | >$10k directional aggregation, no offset, DST cutoff tests, one working beneficiary/day candidate, due date, recalculation, filed-impact tasks | Multiple properties/legal entities, joint-beneficiary graph aggregation, per-channel legal exceptions, calendar/scope change migrations |
| CTR Review | Source drilldown, encrypted revisions, validation, review/approve/return/reject, independent approver, approval invalidation, corrections | Actual FinCEN form field/schema implementation and supported production-file generation |
| CTR Batch / History | Frozen version membership, mock export checksum, simulator submission, item outcomes, raw response, correction links, unresolved rejection task | Real filing transport, real acknowledgment import, repeated transport-attempt ledger |
| SAR | Restricted Review/Batch/History, manual synthetic narrative, detection/activity dates, 30-day deadline, additional subjects/evidence, demo referral task, close API, review workflow | 60-day unidentified-suspect workflow, urgent escalation UI, continuing activity scheduler, complete narrative/evidence/investigation case model |
| Tax | Source-linked events, demo W-2G/1099-MISC eligibility and withholding, validation/recalculation, independent review, mock batch/history, recipient text copy, W-9 lifecycle, nonresident referral, 1098 rejection | Complete IRS tax-year tables, state/backup/foreign withholding, official form PDFs, adjusted delivery deadline calendars, 1042-S/5754, production IRS export/transport |
| Setup | Property metadata, users/roles, type governance, retained policy proposals and review tasks | Dynamic application/approval of arbitrary legal-rule JSON, full user disable/password-reset UI, multi-property management, production IAM |
| Security | Server permissions, sessions/CSRF, BCrypt, encrypted sensitive columns/evidence, restricted SAR access, masked IDs, immutable audit trigger | Production key rotation/KMS, MFA/SSO, malware scanning, formal threat model/pen test, tenant isolation, audited full-sensitive-identity reveal |
| Operations | Flyway migrations, persistent PostgreSQL, backup command, failure tasks, CI, health endpoint, outbox retry, Node replay deduplication | Certified restore drill, automated legal holds/retention enforcement, queued-job admin UI, measured availability/scale/load targets |
| UI | Responsive six-module navigation, forms, reason confirmations, status badges, tables/search/pagination, errors/empty states, audit drawer, evidence uploads | Server-side pagination and 100k-patron search optimization, fully audited accessibility conformance and focus-trap improvements |
| Companion technologies | Spring Kafka outbox, Node consumer/NDJSON/SSE, C++17 concurrent validator, Swing REST client | Full local Docker/Kafka integration was not run in this environment; native and desktop components do not sit on the main financial processing path |

## Acceptance scenarios

| Scenario | Evidence / boundary |
| --- | --- |
| AC01 single $10,001 | Rules thresholds and PostgreSQL candidate tests |
| AC02 6000+4500 | multipleCashInTriggersOneCandidate |
| AC03 exactly $10,000 | exactThresholdDoesNotTrigger |
| AC04 independent directions | directionsDoNotCombine |
| AC05 both above threshold | bothDirectionsRetained |
| AC06 cutoff / DST | parameterized gamingDay tests and isolated cutoff seed |
| AC07 slot jackpot exclusion | jackpotExcludedAndTaxCreated |
| AC08 no currency | NONCASH type validates zero currency; Rules and seeded fixture |
| AC09 correction / void | correctionPreservesOriginalAndLinksReplacement; voidPreservesRecordAndRecalculates |
| AC10 source retry | idempotencyAndPayloadCollision |
| AC11 concurrent crossing | concurrentThresholdCrossingsAreAtomic using two actual threads/transactions |
| AC12 approved custom type | customTypeActivationNeedsIndependentApprover |
| AC13 inactive type | deactivatedTypeNotAccepted; historical copied values |
| AC14 self approval | creatorCannotSelfApprove |
| AC15 material change | materialEditRevokesApproval; sourceChangeInvalidatesBatchedVersion |
| AC16 item outcomes | partialBatchReconcilesItemsIndependently |
| AC17 SAR security | sarRestrictedAtServer; adminDoesNotGetSarByDefault; attachmentDownloadRespectsSarPermission |
| AC18 preapproval tax changes | taxEditRecalculatesAndForeignCasesStop; source-linked amount reconciliation enforced |
| AC19 invalid 1098 | invalidTaxFormBlocked |
| AC20 separate W-9 | w9CannotEnterReportBatch |
| AC21 preserved filing | submittedSnapshotSurvivesProfileChange |
| AC22 unconfirmed submission | submissionRemainsUnconfirmedUntilAck; exportIsNotSubmissionOrAcceptance |
| AC23 nonresident stop | foreignPayeeReferral; taxEditRecalculatesAndForeignCasesStop |
| AC24 internal rejection | internalRejectionLeavesTask |

Presence of an automated test is not a claim of comprehensive regulatory correctness. VERIFICATION.md records actual executed checks. The 100,000-patron p95 and 20-user posting targets have **not been measured**. A property-row lock ensures demo correctness but is not a high-throughput design certification.

## Production gates

The project intentionally cannot make real regulator submissions. Jurisdictional scope, current filing schemas, complete tax tables, supported adapters, operational approvals, external credentials, retention/key management, and security/load validation remain mandatory before any real use. No employer systems or metrics are reproduced here.
