# Persistence and record design

Flyway SQL is authoritative: `backend/src/main/resources/db/migration`.

| Table | Purpose / controls |
| --- | --- |
| cage_user | Unique username, BCrypt hash, explicit role set, enabled flag |
| property | Single reporting scope, immutable demo timezone and cutoff; posting lock |
| patron | Profile, encrypted synthetic ID/TIN, identity/W-9 status, merge survivor FK |
| transaction_type | Unique code, currency mapping, approval/effective dates, active flag |
| cage_transaction | Unique source ID; beneficiary/conductor/type/correction FKs; fixed precision amounts; recorded rule/type versions |
| report | CTR/SAR/TAX, unique kind/group key, independent internal/filing/external states, approved revision, encrypted draft and frozen snapshot |
| report_revision | Immutable-by-application report snapshots with actor/reason |
| report_subject | Additional patron FKs and report relationship; unique membership |
| filing_batch | Mock schema, transport receipt, checksum, response payload, timestamps |
| batch_item | Report/version membership uniqueness and encrypted frozen snapshot; item-specific acknowledgment |
| attachment | Encrypted evidence payload, owner pointer verified by API, filename/type/size |
| audit_event | Actor, reason, before/after encrypted values, timestamp and correlation ID; append-only trigger |
| work_task | Explicit unresolved compliance work and resolution |
| configuration | Versioned policy proposal JSON; not an unreviewed override of regulatory mappings |
| outbox_event | Committed synthetic transaction events, delivery time and retry count |

Every row has a bigint primary key and optimistic `version`. Money uses numeric(38,2) plus Java BigDecimal. Timestamps use timestamptz; gaming-day labels use date. Indexes support patron/day/status lookup, queues, audit ownership and pending outbox events.

Deliberate simplifications: one property/legal scope, USD, one primary identity per patron, one transaction type per entry with gross/cash rather than an arbitrary tender-leg table, tax event represented by a TAX report/source link, one attempt per prepared batch with explicit uncertain-result reconciliation. Evidence's polymorphic owner is validated by application rather than a database FK. Tax rules are code-backed versioned fixtures; policy edits are retained as proposals. Read the requirements coverage for remaining entities and workflows.
