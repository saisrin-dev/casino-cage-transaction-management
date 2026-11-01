# Five-minute portfolio walkthrough

1. Open CageLedger as `cashier`. Show the role-specific navigation and synthetic-data banner. Create a synthetic patron, enter a $6,000 chip purchase followed by $4,500 using different source IDs. Explain why actual currency, not the gross/noncash amount, drives reportable totals.
2. Switch to `analyst`. Open CTR Review, inspect the $10,500 cash-in candidate and linked transactions. Explain the 06:00 New York gaming-day cutoff and why cash-out cannot offset cash-in. Validate and send for approval.
3. Switch to `approver`. Review the same revision and approve with a reason. Explain separation of duties and why a material change would invalidate approval.
4. Switch to `filer`. Select approved reports, prepare a mock batch, export the demonstration JSON, submit to the PARTIAL simulator, then reconcile. Point out that export, transport and external acknowledgment have different statuses. Rejected items stay visible and need a linked correction.
5. Open restricted SAR Review as `analyst` and show a synthetic narrative/evidence. Show that `cashier` and `admin` do not have SAR access. Open tax/W-9 as `tax` and explain the separate IRS workflow and visible DEMO RULES limitation.

Technical discussion: REST services, PostgreSQL/Flyway, BigDecimal, row locking/idempotency, versioned reports, encrypted evidence and session/CSRF protection. Locally enable the Kafka profile to show transaction outbox delivery and Node's deduplicated SSE stream. Demonstrate the separate C++ and Swing examples if relevant to the interview.

Describe this accurately as an independent portfolio project inspired by your domain experience. Do not claim real filing capability, employer transaction volumes, validated regulatory completeness, production availability or measured performance.
