# Complete project generation prompt

Act as a senior full stack engineer and build the complete runnable Casino Cage Transaction and Compliance System specified below. Generate source code, not just an architecture or plan. Use Java 17, Spring Boot 3, Spring Security, Spring Data JPA, Maven, PostgreSQL, Flyway, React with TypeScript, Vite, and Material UI. Use Docker Compose for reproducible local startup. Pin mutually compatible maintained dependency versions when generating the project. Use a modular monolith with clear controllers, services, repositories, DTOs, validation, and centralized error handling.

Build all six modules and every screen in the specification. Implement backend logic and persistent database behavior behind each action. Do not use placeholder buttons, TODO implementations, fabricated real submissions, or browser local storage as the main database. Use a server-authoritative REST API and role-based security. Keep regulator integrations behind interfaces with fully functional simulation adapters and explicit production adapter boundaries.

Deliver the following:

1. A complete repository with backend, frontend, database migrations, Dockerfiles, docker-compose.yml, .env.example, README, and a file tree.
2. One-command local startup with Docker Compose; document individual backend and frontend startup as alternatives. Read secrets from environment variables. Use a development-only seed profile for synthetic users and data.
3. Persistent entities, foreign keys, indexes, unique constraints, report versions, audit records, seed migrations, and schema documentation. Use BigDecimal for money, date/time types with property time zone rules, and optimistic locking or appropriate transactional concurrency controls.
4. REST endpoints for patron CRUD and search, identity records, transaction entry/correction/void, type configuration, aggregation details, report review/approval/rejection, batch creation/submission/response reconciliation, history, tax events and forms, configuration, authentication, and permission-controlled exports. Publish an OpenAPI specification with working examples.
5. A responsive frontend with sidebar navigation, searchable paginated tables, patron selection, dropdowns, forms, inline validation, confirmation dialogs for voids, status badges, review work queues, details pages, batch drilldowns, and complete loading, empty and error states. Show simulated filing labels wherever submission is possible.
6. A deterministic mock filing service for FinCEN and IRS with scenarios for acceptance, warnings, rejection, partial batch results, delayed response and transport timeout. Simulated IDs must have a MOCK prefix. Allow controlled response simulation only in development. Production adapters must fail closed when not implemented.
7. Unit and integration tests for monetary aggregation, gaming-day boundaries and daylight saving, report state transitions, authorization, approval invalidation, reversals, idempotency, duplicate imports, custom type activation, tax form eligibility and mocked submission reconciliation. Use JUnit and PostgreSQL-backed integration tests where relevant. Include Playwright UI smoke tests for critical user journeys.
8. Synthetic seed data: at least 20 patrons, multiple cashiers and compliance users, 100 transactions spanning multiple gaming days, default transaction types, reviewable CTRs and SARs, tax forms, successful and rejected batches, and all major acceptance-test boundary cases. Keep scenario fixtures isolated so extra seed activity does not alter the expected totals. Do not use real TINs, personal data, filing credentials, or copied confidential SAR narratives.
9. A README with prerequisites, environment variables, exact commands, development accounts, module walkthrough, test commands, business rules, security model, mock filing behavior, known limitations, and a verification checklist. Include a script or documented command that creates a distributable project ZIP.
10. An accessible professional interface with readable forms, keyboard navigation, USD formatting, explicit date/time zone labels, masked sensitive information, useful errors, and an audit history drawer.

Implement real server-side validation. Do not trust client-calculated amounts or role flags. For the runnable development build, use seeded effective-dated demonstration tax rules with tests and visible DEMO RULES labels; do not claim these are a production-ready complete tax determination engine. Keep form templates and rule tables versioned. Use truthful status labels: export, transport submission and regulator acknowledgment are distinct.

Work in implementation phases without dropping requirements. If you have a coding workspace, create the files, run the builds and tests, fix failures, and provide the finished project archive. If limited to chat, provide complete files with exact paths in numbered parts, stop only at file boundaries, and retain a manifest showing completed and remaining files. Never silently replace code with ellipses. State verification actually performed and any unresolved work accurately.

Use the complete functional requirements below as the source of truth. Proceed with reasonable documented defaults; the demo does not require real regulator access.

---

# Casino Cage Transaction and Compliance System Requirements

Version 1.0 | Research checked October 31 2026

## 1 Purpose and scope

Build a US casino cage application that manages patron profiles, cage transactions, Currency Transaction Reports (CTRs), Suspicious Activity Reports (SARs), jackpot tax documents, and configuration. The application must connect each transaction and report to its underlying patron and preserve a complete audit trail.

This specification defines proposed product behavior. Regulatory facts are identified through sources S1–S9 below; screen layouts, internal approval controls, and performance targets are product design decisions. The baseline assumes a casino subject to the federal casino BSA rules. State and tribal requirements require separate rule profiles. The first project release uses synthetic data and simulated filings; production filing requires approved credentials, supported filing specifications, operational controls, and compliance validation.

## 2 Modules and navigation

| Module | Screens |
| --- | --- |
| Subject Manager | Patron Search, Create Patron, Patron Details, Edit Patron, Profile History |
| Transaction Manager | Transaction Search, Transaction Entry, Transaction Details, Edit or Void Transaction |
| CTR Manager | CTR Review, CTR Batch, CTR History |
| SAR Manager | SAR Review, SAR Batch, SAR History |
| Tax Forms Manager | Tax Review, Tax Form Editor, Tax Batch, Tax History, W-9 Register |
| Setup Manager | Property Settings, Transaction Settings, Reporting Rules, Tax Rules, Users and Roles, Filing Settings, Audit Settings |

The dashboard shows today's activity, separate cash-in and cash-out totals, pending reviews, filing deadlines, rejected submissions, and outstanding acknowledgments. SAR counts and links are visible only to authorized SAR users.

## 3 Users and permissions

Roles: Cage Cashier, Cage Supervisor, Compliance Analyst, Compliance Approver, Authorized Filer, Tax Specialist, Administrator, and Read Only Auditor.

- Cashiers maintain permitted patron information and enter cage transactions.
- Supervisors authorize posted-transaction corrections and voids.
- Analysts prepare CTRs and SARs according to their permissions.
- Approvers approve, return, or internally reject reports with a reason.
- Filers assemble batches, export or transmit filings, and reconcile responses.
- Tax specialists manage tax forms and withholding information.
- Administrators maintain configuration; this role does not automatically grant SAR access.
- Auditors see only records allowed by their assigned scope.
- By default, creators cannot approve their own reports. All permission checks apply at the server, including exports and attachments.

## 4 Subject Manager requirements

SUB-01 Create a patron profile with a unique internal patron ID. Capture legal name, aliases, date of birth, residential address, country, contact details, occupation or business, taxpayer identification information where applicable, identification type, issuing authority, ID number, issue and expiry dates, and loyalty account.

SUB-02 Search by patron ID, name, date of birth, loyalty ID, or permitted identification fields. Mask sensitive values in results.

SUB-03 Detect potential duplicate profiles using configurable matching. An authorized merge preserves both original IDs, documents the decision, and triggers affected aggregation recalculation.

SUB-04 Edit permitted profile fields with field-level before and after history, operator, timestamp, and reason. Submitted reports retain the exact patron snapshot filed.

SUB-05 Display linked transactions and tax documents. Display CTRs and SARs only when the user has the corresponding report permissions.

SUB-06 Store verified, unverified, expired, unavailable, and refused identity information distinctly. Permit schema-supported unknown indicators; never fabricate missing data. Hold transactions or reports when applicable policy requires verification.

SUB-07 Support a person conducting a transaction on behalf of another person or entity. Record conductor, beneficiary, and relationship separately. Do not aggregate solely by loyalty account or cashier-selected profile.

## 5 Transaction Manager requirements

### 5.1 Transaction entry screen

TXN-01 Require patron selection before ordinary named-patron entry. Provide quick search and permission-controlled Create Patron. Provide a supervised exception workflow for unidentified activity with temporary identity and later reconciliation; missing identity must remain visible to compliance.

TXN-02 Capture transaction ID, source event ID, patron, conductor and beneficiary if applicable, property, cage or station, cashier, event timestamp and time zone, gaming day, transaction type, tender components, gross amount, cash amount, original currency, exchange rate and USD equivalent when applicable, account or marker reference, machine or table reference, jackpot ID, notes, and attachments.

TXN-03 Derive cash direction and reporting treatment from the approved transaction definition. Display cash-in, cash-out, excluded, or noncash treatment to the operator. Split mixed-tender transactions into their actual cash and noncash components.

TXN-04 Support Save Draft, Post, Edit Draft, Correct Posted Transaction, Void, View History, and Print Receipt. Calculate monetary values using decimal arithmetic.

TXN-05 The Delete action removes a draft from active work or voids a posted transaction according to permission. Preserve posted records and reversal links; never silently erase financial activity. Require a reason and approval for posted corrections or voids.

TXN-06 Recalculate affected gaming-day totals and report candidates after posting, backdating, correcting, voiding, identifying an unknown patron, or merging profiles. A reversed entry contributes no active amount, while its audit evidence remains.

TXN-07 Detect repeated source IDs and retries so imports or double clicks cannot create duplicate activity. Protect simultaneous updates and threshold crossings with atomic processing.

### 5.2 Default transaction type dropdown

The following are realistic starter entries grounded in the casino transaction categories in S1–S3. Treatment applies to the actual currency component, subject to the rule's exceptions. Listing a transaction does not mean the application executes a bank transfer or authorizes casino credit.

| Transaction type | Default currency treatment | Additional data |
| --- | --- | --- |
| Chip or token purchase with cash | Cash-in | Table and instrument details |
| Chip or token redemption for cash | Cash-out | Redemption reference |
| Front money deposit | Cash-in | Account reference |
| Front money withdrawal | Cash-out | Account reference |
| Safekeeping deposit | Cash-in | Safekeeping reference |
| Safekeeping withdrawal | Cash-out | Safekeeping reference |
| Marker or credit repayment in cash | Cash-in | Marker reference |
| Marker or credit advance paid in cash | Cash-out | Credit approval reference |
| Cash wager or money play | Cash-in subject to exceptions | Table and wager reference |
| Table game payout in cash | Cash-out subject to exceptions | Game and payout reference |
| Cash received to fund outgoing wire | Cash-in | Wire reference |
| Incoming wire proceeds paid in cash | Cash-out | Wire reference |
| Casino check purchased with cash | Cash-in | Check reference |
| Personal or other eligible check cashed | Cash-out | Check details |
| Currency exchange or bill exchange | Capture both cash-in and cash-out legs; apply applicable exceptions | Currency pair and rate |
| TITO ticket redemption for cash | Cash-out | Ticket validation ID |
| Slot or video lottery jackpot payout | Excluded from casino CTR under the jackpot exemption | Jackpot, machine and tax linkage |
| Tournament or promotional cash payout | Cash-out where covered | Prize classification |
| Cash travel reimbursement or gaming incentive | Cash-out where covered | Expense or promotion reference |
| Electronic gaming device cash insertion | Conditional aggregation treatment | Machine event and known patron linkage |
| Noncash transfer, check-only payment or cashless wallet movement | No currency component unless actual cash is involved | Payment and account references |

Slot jackpot exclusion does not automatically apply to a subsequent safekeeping deposit or other separate transaction. Preserve the distinction between a jackpot and its later movements. CTR exclusions do not exclude suspicious activity review. [S2]

### 5.3 Custom transaction types

TXN-08 Setup Manager > Transaction Settings must support Add, Edit, Clone, Activate, and Deactivate. Define unique code, label, description, supported tender types, cash legs, report category, inclusion or exception rules, tax event mapping, required fields, permitted roles, and effective dates.

TXN-09 Custom types require compliance approval before activation. An operator cannot create a type to bypass CTR reporting. Deactivated types disappear from new-entry dropdowns but remain readable historically. Historical transactions retain their original definition version. Reclassification requires an audited recalculation process.

## 6 Gaming day and CTR rules

CTR-01 Configure a property's gaming-day boundary and time zone. A gaming day is not assumed to equal a calendar day. An illustrative 06:00 boundary groups events from 06:00 through just before 06:00 the next day. Handle daylight saving changes using timestamps and property-local boundaries.

CTR-02 Aggregate reportable currency activity for a person, including known activity conducted on their behalf, across relevant channels and departments in the reporting casino scope. Group by gaming day and direction. Do not combine unrelated patrons or unrelated legal reporting entities.

CTR-03 Create a CTR candidate when either reportable cash-in or reportable cash-out exceeds $10,000 in that gaming day. Exactly $10,000 does not cross this threshold. Never offset cash-out against cash-in or sum the two directions into one trigger. A single transaction or multiple transactions can trigger reporting. [S1, S2]

CTR-04 Maintain one working report group per reporting scope and gaming day with the appropriate subject relationships. Further activity updates an unfiled report rather than creating duplicate drafts. Current form instructions determine report subject structure and the population of both direction fields.

CTR-05 Show contributing and excluded transactions, exclusion reasons, cash totals, rule version, report date, and deadline. The CTR filing deadline is 15 calendar days after the reportable transaction date; approval delays do not reset it. [S1]

CTR-06 Late corrections to filed activity create a compliance task to assess corrected or amended filing. Never overwrite the submitted report or automatically discard an outstanding obligation because a user rejected a draft.

## 7 CTR Manager screens

### 7.1 CTR Review

- Search and filter by patron, gaming day, cash direction, status, property, assignee, and due date.
- Open a prepopulated CTR with filing institution, transaction information, subjects and conductors, identification, cash categories, and filing metadata aligned to the current FinCEN schema.
- Drill into linked transactions and compare totals to the report fields.
- Permit supported form-field edits with validation, reason, and audit history. Transaction-derived amounts require correction of source data or a controlled, reconciled adjustment rather than an unexplained override.
- Actions: Save Draft, Validate, Send for Approval, Approve, Return for Correction, Reject Internally, and Select for Batch.
- Reject Internally requires reason, authority, and a resolution task where a reportable obligation remains. Internal rejection is distinct from regulator rejection.
- Approval locks the reviewed report version. Material edits revoke approval and require another review.

### 7.2 CTR Batch

- Select approved, unsubmitted report versions; group under a unique batch ID.
- Validate schema, mandatory fields, record totals, report counts, and duplicate membership.
- Show a batch list with date, filer, report count, status, and acknowledgment state. Open a batch to view each included CTR and its submitted snapshot.
- Export the supported filing file for authorized filing, or transmit through a separately implemented supported integration. Do not assume a public REST filing API.
- Preserve file checksum, schema version, export, transport receipt, report identifiers, errors, and responses. Exported is distinct from Submitted.
- Import or reconcile acknowledgments and track each report independently; allow for differing results within a batch. Retry safely and reconcile uncertain transmission results before retransmission. [S4]

### 7.3 CTR History

Show submitted, acknowledged, warning, externally rejected, internally rejected, corrected, and superseded records with filters, original snapshots, approval history, source transactions, batch details, BSA IDs where returned, and correction chains. Store submitted and accepted as separate facts.

## 8 SAR rules and screens

SAR-01 Permit manual creation from SAR Review by selecting a patron and entering suspicious activity information. Also support unknown subjects when permitted by the filing schema, multiple subjects, and links to relevant transactions and CTRs.

SAR-02 CTR generation does not automatically create a SAR. A casino SAR obligation generally involves conducted or attempted suspicious activity totaling at least $5,000 in funds or other assets and applicable suspicion criteria. Support voluntary reports and document the analyst's decision. [S5]

SAR-03 Provide configurable referral patterns such as possible structuring, minimal play followed by redemption, unexplained third-party activity, conflicting identification, unusual account movement, and suspected bill stuffing. These create analyst alerts, not automatic findings of wrongdoing.

SAR-04 Capture initial detection date, activity start and end dates, suspicious amount, subject details, activity categories, narrative, investigation notes, filing institution, contacts, related reports, and evidence references. Preserve supporting documents internally and do not attach them to a filing unless its current specification permits this.

SAR-05 Track a 30-calendar-day filing deadline from initial detection of facts that may support filing. If no suspect is identified, support the permitted additional identification period, never more than 60 days from initial detection. Record the basis; approval does not restart the clock. Provide an urgent escalation workflow. [S5]

### 8.1 SAR Review

Provide Create SAR, Save Draft, Validate, Send for Approval, Approve, Return for Correction, Reject Internally or Close Without Filing, and Select for Batch. Require a documented investigation outcome for closure. Restrict narratives, SAR existence, attachments, exports, and notifications to authorized users. Do not reveal SAR filing to patrons. [S5]

### 8.2 SAR Batch

Batch approved SAR versions, validate the current schema, export or transmit through supported filing channels, list included reports, and reconcile transport and acknowledgment results. Store BSA IDs, report errors, warnings, batch membership, and retry history. Treat continuing activity and corrections as explicit filing types according to the current instructions.

### 8.3 SAR History

Display submitted, acknowledged, internally rejected, externally rejected, corrected, continuing activity, and closed without filing outcomes. Include narratives, evidence links, decision history, and report relationships subject to SAR permissions.

## 9 Tax Forms Manager

### 9.1 Form selection and regulatory boundaries

Tax documents go through IRS reporting workflows, separately from FinCEN BSA filing. Do not submit W-9 or IRS returns in a CTR or SAR batch.

| Form | Product treatment |
| --- | --- |
| W-2G | Primary gambling-winnings reporting form when game-specific reporting or withholding conditions apply |
| W-9 | Collect taxpayer identification and certification; retain for payer records rather than treating it as a jackpot information return |
| 1099-MISC | Offer for qualifying non-wager promotional prizes or other applicable payments; select the specific variant rather than generic 1099 |
| 1098 | Mortgage Interest Statement; disabled for jackpot winnings. Include only in a separately justified future mortgage workflow |
| 1042-S and associated nonresident documentation | Extended scope for applicable foreign-payee reporting and withholding; route such patrons to specialist review until implemented |
| 5754 | Extended scope for applicable group or shared gambling winnings |

These distinctions follow IRS sources S6–S9. A user may choose an eligible form, but cannot bypass applicability rules by selecting an arbitrary form type.

TAX-01 Create a tax event from a jackpot or eligible prize payment. Link patron, source transaction, jackpot ID, machine or game, payment date, tax year, payer, jurisdiction, residency classification, gross winnings, wager where relevant, reportable amount, federal and state withholding, and net payment.

TAX-02 Provide an editable form with legal name, address, identification, eligible form type, winnings, wager, withholding, and supported form-specific fields. Display calculation explanations and validation errors.

TAX-03 Permit winning-amount changes before approval with a reason and source reconciliation. Recalculate dependent values. An already filed form is corrected through a new linked version rather than overwritten. Changing form type requires eligibility validation and renewed approval.

TAX-04 Configure rules by effective date, tax year, game, payer and jurisdiction, including amount thresholds, wager ratios, reporting basis, withholding and recipient deadlines. Do not use the CTR $10,000 threshold for tax reporting.

TAX-05 The January 2026 W-2G instructions identify a $2,000 minimum threshold for certain 2026 reporting, with game-specific rules and exceptions. Load the complete applicable IRS rule table; do not implement one universal jackpot threshold. Legacy tax years retain their own rules. Validate the 2026 table against Publication 1099 and the current W-2G instructions before enabling production. [S6, S9]

TAX-06 Keep regular withholding, backup withholding, state withholding and foreign-payee treatment separate. Do not automatically withhold the same rate on every jackpot. The applicable IRS instructions determine eligibility and calculation. [S6]

TAX-07 Tax Review supports draft, validation, independent approval, rejection with reason, batch selection, and recipient copy generation. Tax Batch and Tax History retain original filings, corrections, IRS responses, delivery status, and rejected records. W-9 has a separate requested, received, verified, and replacement lifecycle; it is not submitted as an information-return batch.

## 10 Setup Manager

CFG-01 Configure property and reporting institution details, cage locations, local time zone, gaming-day start, filing contacts, and legal reporting scope.

CFG-02 Configure transaction types, tenders, mandatory fields, cash mappings, exemptions, roles, custom types, and effective dates.

CFG-03 Configure CTR aggregation scope, alerts below threshold, review assignments, due-date escalation, and legal rule profiles. Internal warning thresholds may differ from mandatory filing thresholds. Ordinary administrators cannot raise a statutory threshold to suppress required reporting.

CFG-04 Configure SAR referral patterns, categories, assignment, narrative prompts, deadline alerts, escalation, and restricted permissions.

CFG-05 Configure tax forms, templates, tax-year thresholds, game rules, withholding rules, corrections, recipient delivery, and specialist referrals.

CFG-06 Configure approval permissions, supervisor controls, filing schemas, environment, acknowledgment processing, retention, and audit access. Require maker and approver controls for regulatory mappings. Record who changed a rule, why, when it became effective, and what reports it affects.

## 11 Status and data design

Keep report workflow status separate from external submission status.

| Status field | Proposed values |
| --- | --- |
| Transaction | Draft, Posted, Corrected, Voided |
| Internal report | Draft, Pending Review, Returned, Approved, Rejected Internally, Closed Without Filing where applicable, Superseded |
| Filing attempt | Prepared, Exported, Queued, Submitted, Awaiting Response, Response Received, Transport Failed |
| External outcome | Unconfirmed, Accepted, Accepted With Warnings, Rejected, Acknowledgment Failed or other mapped source value |

Store the raw external response alongside normalized internal values. The filing channel's status vocabulary governs the mapping; do not infer acceptance merely from a submitted label. [S4]

Core entities: Patron, PatronIdentity, PatronRelationship, Property, GamingDayRuleVersion, TransactionTypeVersion, Transaction, TransactionTenderLeg, Reversal, AggregationResult, CTR, SAR, ReportSubject, ReportTransactionLink, ReportVersion, Approval, Batch, BatchItem, FilingAttempt, Acknowledgment, TaxEvent, TaxForm, W9Certification, Attachment, RuleVersion, and AuditEvent.

## 12 Security and operational requirements

- Encrypt sensitive data in transit and at rest. Mask TINs and ID numbers and record sensitive access and exports.
- Keep immutable audit events for changes, approvals, closures, rule changes, batches, and responses. Include actor, timestamp, reason, old and new values, and correlation ID.
- Retain filed CTRs for five years and filed SARs plus supporting documentation for five years from filing. Apply other record-specific rules and legal holds; configure tax retention separately. [S1, S5]
- Isolate test and production. All demo filings and generated forms must be clearly marked as simulations. Use synthetic patrons and no real submission credentials in the portfolio project.
- Maintain backup, restore, reconciliation, and failed-job queues. Transaction posting must not depend on regulator availability.
- Proposed project targets: patron search under two seconds at the 95th percentile for 100,000 synthetic patrons; single transaction posting and aggregation under three seconds at the 95th percentile under a documented 20-user test workload. Measure and report these as test targets, not proven capacity.
- Display queues for missing identity, late transactions, overdue reports, failed submissions, and missing acknowledgments. Prevent confidential SAR details from leaking through shared notifications or patron receipts.

## 13 Acceptance scenarios

| ID | Scenario | Expected result |
| --- | --- | --- |
| AC01 | Patron posts one eligible $10,001 cash-in | One working CTR candidate |
| AC02 | Patron posts $6,000 and $4,500 eligible cash-in in one gaming day | Cash-in totals $10,500 and triggers CTR |
| AC03 | Eligible cash-in is exactly $10,000 | No threshold-generated CTR |
| AC04 | $7,000 cash-in and $6,000 cash-out | No CTR solely from summing directions |
| AC05 | $12,000 cash-in and $11,000 cash-out | Both directional totals retained without offset |
| AC06 | Activity falls on opposite sides of gaming-day cutoff | Separate day totals |
| AC07 | $12,000 direct slot jackpot payout | Apply jackpot CTR exemption; evaluate tax and suspicious activity separately |
| AC08 | Noncash $12,000 event with no currency leg | No CTR solely because the total exceeds $10,000 |
| AC09 | Correct or void a contributing posted transaction | Audited reversal or correction, recomputed totals, report impact task |
| AC10 | Same source event retried | One transaction and one aggregation contribution |
| AC11 | Two cashiers post threshold-crossing events together | Correct total with no duplicate candidate |
| AC12 | Active custom cash-in transaction type selected | Correct fields, mappings and aggregation |
| AC13 | Custom type deactivated | Hidden from new entry; historical labels remain |
| AC14 | Creator tries to self approve | Blocked under default separation of duties |
| AC15 | Approved report amount changes | Approval revoked; review required |
| AC16 | Submitted batch has report-specific errors | Item-level outcomes and correction work retained |
| AC17 | Cashier without SAR permission opens a SAR URL or export | Access denied without disclosing SAR details |
| AC18 | Jackpot amount edited before approval | Validated recalculation and source reconciliation |
| AC19 | User selects 1098 for jackpot | Blocked with applicability explanation |
| AC20 | W-9 completed | Stored in W-9 register; excluded from IRS filing batch |
| AC21 | Posted historical profile is corrected | Filed snapshot unchanged; linked current profile updated |
| AC22 | Form marked submitted without acknowledgment | Remains externally unconfirmed |
| AC23 | Nonresident tax case without implemented rules | Specialist review; no default resident form auto filing |
| AC24 | Internal CTR rejection despite remaining reporting obligation | Reason recorded and unresolved compliance task remains open |

## 14 Delivery phases

Phase 1: Subject Manager, configurable transactions, gaming-day aggregation, CTR review and simulated batch/history, user permissions and audit trail.

Phase 2: SAR review and simulated batch/history, restricted access, tax events, W-9 register, W-2G editor and tax-year rules, custom transaction approval.

Phase 3: Advanced tax cases, multi-source imports, reconciliation, supported production filing adapters, acknowledgment ingestion, and state or tribal overlays.

Before production, confirm casino reporting eligibility, jurisdiction, legal entity scope, gaming-day boundaries, complete tax-year rule tables, approved form revisions, filing channel specifications, correction procedures, and retention schedules. This does not block building the synthetic-data project.

## 15 Official research sources

- S1 — IRS casino reporting overview: https://www.irs.gov/government-entities/indian-tribal-governments/itg-faq-8-answer-what-are-the-reporting-requirements-for-casinos
- S2 — FinCEN casino recordkeeping and reporting FAQ: https://www.fincen.gov/resources/statutes-regulations/guidance/frequently-asked-questions-casino-recordkeeping-reporting
- S3 — Federal Reserve reproduction of 31 CFR 1021.311 transaction categories and exceptions: https://www.federalreserve.gov/frrs/regulations/section-1021311-filing-obligations.htm
- S4 — FFIEC BSA E-Filing appendix and acknowledgment status explanations: https://bsaaml.ffiec.gov/manual/Appendices/21 . Use current FinCEN filing specifications for actual integrations: https://bsaefiling.fincen.gov/
- S5 — eCFR casino suspicious transaction reporting rule 31 CFR 1021.320: https://www.ecfr.gov/current/title-31/subtitle-B/chapter-X/part-1021/subpart-C/section-1021.320
- S6 — IRS W-2G and 5754 instructions, January 2026 revision: https://www.irs.gov/instructions/iw2g
- S7 — IRS W-9 purpose: https://www.irs.gov/forms-pubs/about-form-w-9
- S8 — IRS Form 1098 purpose: https://www.irs.gov/forms-pubs/about-form-1098
- S9 — IRS Publication 1099 for 2026, including reporting categories and thresholds: https://www.irs.gov/publications/p1099

Research note: IRS overview pages can retain older amounts while revised instructions reflect newer tax years. The implementation must use the applicable year-specific instructions and effective dates, not scrape a single overview amount or adopt a future-dated revision automatically.
