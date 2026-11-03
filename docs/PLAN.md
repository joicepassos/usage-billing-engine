# Implementation plan — usage-billing-engine MVP
Source: docs/PRD.md v1.1 · Status: approved · Date: 2026-11-02

## Overview
Foundations first (a green build, recorded decisions, shared value objects, tenancy), then a spike on the two external contracts because they carry the most uncertainty. After that the plan follows the money: usage in, pricing, cycle close, invoice, NFS-e, charge, payment. The simulator and observability close the loop for the portfolio demo.

```
T-01 build ─ T-02 ADRs ─ T-03 shared kernel ─┬─ T-04 spike (external contracts)
                                              ├─ T-05 tenancy ─ T-06 customers
                                              ├─ T-07 ingestion ─ T-08 aggregation
                                              └─ T-09 pricing ─ T-10 plans & subscriptions
T-06 + T-08 + T-10 ─ T-11 cycle close ─ T-12 unknown customers
T-11 ─ T-13 finalization ─ T-14 outbox ─ T-15 NFS-e ─ T-16 rejection
T-15 ─ T-17 charge ─ T-18 reconciliation ─ T-19 overdue ─ T-20 cancellation
T-21 queries · T-22 simulator · T-23 observability · T-24 readiness
```

Every task ends with `./gradlew check` and a self-review with `code-review`.

## Tasks

### T-01 — Make the build and CI green · Size: S · Role: backend-engineer
- **Goal:** confirm the bootstrap compiles and `ModularityTest` passes; fix any wrong Spring Boot 4 starter coordinates or versions.
- **Covers:** NFR reproducibility
- **Module:** —
- **Likely files:** `build.gradle.kts`, `gradle/libs.versions.toml` (edit)
- **Depends on:** —
- **Parallel with:** T-02
- **Done when:** GitHub Actions run on `main` is green.
- **Risks:** coordinates chosen without a local build. Recovery: revert to the last green commit.

### T-02 — Record approved decisions as ADRs · Size: S · Role: architect
- **Goal:** turn the decisions in `project-context.md` into ADRs and settle the tier boundary convention.
- **Covers:** AC-05 (convention), all (traceability)
- **Module:** —
- **Likely files:** `docs/adr/0001-modular-monolith.md` … `0009-tier-boundaries.md` (create), `CONTEXT.md` (edit)
- **Depends on:** —
- **Parallel with:** T-01
- **Done when:** ADRs 0001–0009 merged; `CONTEXT.md` defines tier boundaries.
- **Risks:** none (documents only).

### T-03 — Shared kernel and API foundations · Size: M · Role: backend-engineer
- **Goal:** `Money`, `BillingPeriod` (calendar month in `America/Sao_Paulo`), `Clock` bean, global `ProblemDetail` handler with `requestId`, Testcontainers base configuration with TimescaleDB.
- **Covers:** AC-02 (error format), AC-08 (rounding primitive)
- **Module:** `shared` (open module)
- **Likely files:** `.../billing/shared/**` (create), `src/test/.../TestcontainersConfiguration.java` (create), `libs.versions.toml` (edit: Testcontainers modules)
- **Depends on:** T-01
- **Parallel with:** —
- **Done when:** unit tests for `Money` rounding and period boundaries pass; an `@SpringBootTest` context starts against Testcontainers.
- **Risks:** money primitives are high risk. Recovery: property tests before anyone depends on them.

### T-04 — Spike: fiscal issuer and payment gateway contracts · Size: M · Role: architect
- **Goal:** capture request/response shapes for NFS-e issuance (national standard docs) and Asaas sandbox (charge, cancel, webhook) as fixtures; decide webhook authentication.
- **Covers:** de-risks AC-21–AC-27
- **Module:** `fiscal`, `payments` (test fixtures only)
- **Likely files:** `src/test/resources/fixtures/fiscal/**`, `src/test/resources/fixtures/asaas/**` (create), `docs/adr/0010-external-contracts.md` (create)
- **Depends on:** T-02
- **Parallel with:** T-05, T-07, T-09
- **Done when:** fixtures committed with no personal data; ADR 0010 accepted.
- **Risks:** needs an Asaas sandbox account (owner approval). Recovery: build fixtures from public docs and mark them unverified.

### T-05 — Tenants and API key authentication · Size: M · Role: backend-engineer
- **Goal:** provision a tenant via a command, hash and verify API keys, resolve the tenant in a security filter, `@TenantId` on entities, architecture test for tenant-owned entities.
- **Covers:** AC-01, AC-02, AC-03
- **Module:** `accounts` (new, see pending decisions)
- **Likely files:** `.../billing/accounts/**` (create), `db/migration/V2__accounts.sql` (create), `libs.versions.toml` (edit: Spring Security)
- **Depends on:** T-03
- **Parallel with:** T-07, T-09
- **Done when:** integration tests cover 401 without key, 404 across tenants, key shown once; architecture test fails on an entity without `@TenantId`.
- **Risks:** auth is high risk. Recovery: feature is additive; revert the commit.

### T-06 — Customers with fiscal data · Size: S · Role: backend-engineer
- **Goal:** create, update and fetch customers with CPF/CNPJ validation and address; personal data masked in logs.
- **Covers:** AC-04
- **Module:** `accounts`
- **Likely files:** `.../accounts/**` (edit), `db/migration/V3__customers.sql` (create)
- **Depends on:** T-05
- **Parallel with:** T-08, T-10
- **Done when:** integration tests for valid creation and 422 cases pass.
- **Risks:** LGPD. Recovery: masking tested in the same task.

### T-07 — Batch event ingestion · Size: M · Role: backend-engineer
- **Goal:** billable metrics API, `POST /v1/events` (≤ 500 per batch) into a hypertable, idempotency through the `ingested_transaction` ledger, billing period assigned at ingestion (ADR 0003, amendment 1).
- **Covers:** AC-10, AC-11, AC-12
- **Module:** `metering`
- **Likely files:** `.../metering/**` (create), `db/migration/V4__metering.sql` (create)
- **Depends on:** T-03 (T-05 for auth wiring; can stub tenant in tests until then)
- **Parallel with:** T-05, T-09
- **Done when:** integration tests for 202, repeat 200, conflicting 409 within a mixed batch, and a 500-event batch under 500 ms p95 locally.
- **Risks:** idempotency under concurrency. Recovery: unique constraint is the source of truth.

### T-08 — Usage aggregation and late events · Size: M · Role: backend-engineer
- **Goal:** continuous aggregate per customer/metric/day and the current-period usage query; end-to-end tests of late events through the close-facing usage API (the assignment rule itself ships in T-07).
- **Covers:** AC-13, AC-14, AC-16
- **Module:** `metering`
- **Likely files:** `.../metering/**` (edit), `db/migration/V5__usage_aggregate.sql` (create)
- **Depends on:** T-07
- **Parallel with:** T-06, T-10
- **Done when:** tests for in-window, after-close and query freshness pass with a fixed `Clock`.
- **Risks:** aggregate refresh lag at close. Recovery: close reads raw events for the closing period (ADR in T-02 or T-11).

### T-09 — Pricing strategies · Size: M · Role: backend-engineer
- **Goal:** pure domain strategies for per unit, graduated tiers, package and monthly minimum; one rounding point.
- **Covers:** AC-05, AC-06, AC-07, AC-08
- **Module:** `pricing`
- **Likely files:** `.../pricing/**` (create), `libs.versions.toml` (no change; jqwik already present)
- **Depends on:** T-02 (tier convention), T-03
- **Parallel with:** T-05, T-07
- **Done when:** example tests for AC-05–07 and jqwik properties for AC-08 pass.
- **Risks:** money correctness. Recovery: properties guard every later change.

### T-10 — Plans, charges and subscriptions · Size: M · Role: backend-engineer
- **Goal:** APIs for plans with charges and subscriptions; plan change scheduled for the next period.
- **Covers:** AC-09
- **Module:** `pricing`
- **Likely files:** `.../pricing/**` (edit), `db/migration/V6__plans_subscriptions.sql` (create)
- **Depends on:** T-09, T-05
- **Parallel with:** T-06, T-08
- **Done when:** integration test for a plan change on day 15 applying next period passes.
- **Risks:** low.

### T-11 — Cycle close and draft invoices · Size: M · Role: backend-engineer
- **Goal:** scheduled close after the grace window; one draft invoice per subscription with usage or minimum; idempotent reruns; publish `InvoiceDrafted`.
- **Covers:** AC-17, AC-18
- **Module:** `invoicing`
- **Likely files:** `.../invoicing/**` (create), `db/migration/V7__invoices.sql` (create)
- **Depends on:** T-06, T-08, T-10
- **Parallel with:** —
- **Done when:** test closing the same period twice yields exactly one invoice per subscription; zero-usage case yields none.
- **Risks:** double billing. Recovery: unique `(subscription, period)` constraint.

### T-12 — Unknown customers at close · Size: S · Role: backend-engineer
- **Goal:** usage for unregistered customers is skipped with an alert and billed at the next close after registration.
- **Covers:** AC-15
- **Module:** `invoicing`, `metering`
- **Likely files:** `.../invoicing/**`, `.../metering/**` (edit)
- **Depends on:** T-11
- **Parallel with:** T-13 (no shared files if kept in the close job's query)
- **Done when:** scenario test across two periods passes.
- **Risks:** low.

### T-13 — Finalization and operator actions · Size: M · Role: backend-engineer
- **Goal:** auto-finalize after the grace window; operator key; finalize early, hold and release endpoints; publish `InvoiceFinalized`.
- **Covers:** AC-19, AC-20
- **Module:** `invoicing`, `accounts` (operator key)
- **Likely files:** `.../invoicing/**`, `.../accounts/**` (edit)
- **Depends on:** T-11
- **Parallel with:** T-12 (see above)
- **Done when:** tests for auto-finalize and hold behavior pass.
- **Risks:** medium.

### T-14 — Outbox and resilient external calls · Size: M · Role: backend-engineer
- **Goal:** `outbox` table, worker with Resilience4j timeout, retry with backoff and circuit breaker, idempotency keys per external operation.
- **Covers:** AC-22 (mechanism), AC-24 (repeat safety)
- **Module:** `shared` (infrastructure)
- **Likely files:** `.../shared/outbox/**` (create), `db/migration/V8__outbox.sql` (create), `libs.versions.toml` (edit: Resilience4j, WireMock)
- **Depends on:** T-03
- **Parallel with:** T-05–T-13 (no shared files)
- **Done when:** tests show retries on 5xx/timeout, no retry on 4xx, circuit opening and closing.
- **Risks:** new dependencies need approval.

### T-15 — NFS-e issuance with simulator · Size: M · Role: backend-engineer
- **Goal:** `FiscalIssuer` port, WireMock-backed simulator as default, IBS/CBS rate table with validity dates, issuance on `InvoiceFinalized` via the outbox.
- **Covers:** AC-21, AC-22
- **Module:** `fiscal`
- **Likely files:** `.../fiscal/**` (create), `db/migration/V9__fiscal.sql` (create)
- **Depends on:** T-04, T-13, T-14
- **Parallel with:** —
- **Done when:** tests for authorized issuance with correct rates and for recovery after issuer outage without duplicates pass.
- **Risks:** high (external contract). Recovery: simulator isolates the rest of the flow.

### T-16 — Fiscal rejection and reissue · Size: S · Role: backend-engineer
- **Goal:** business rejection sets `fiscal_rejected`, alert, operator reissue after fixing customer data.
- **Covers:** AC-23
- **Module:** `fiscal`, `invoicing`
- **Likely files:** `.../fiscal/**`, `.../invoicing/**` (edit)
- **Depends on:** T-15
- **Parallel with:** T-17
- **Done when:** rejection-then-reissue test passes; no charge created while rejected.
- **Risks:** medium.

### T-17 — Payment charge in Asaas sandbox · Size: M · Role: backend-engineer
- **Goal:** `PaymentGateway` port, Asaas adapter (sandbox profile) and local stub (default), charge on `NfseAuthorized`, invoice `issued` with Pix/boleto data and due date.
- **Covers:** AC-24
- **Module:** `payments`
- **Likely files:** `.../payments/**` (create), `db/migration/V10__payments.sql` (create)
- **Depends on:** T-04, T-14, T-15
- **Parallel with:** T-16
- **Done when:** contract tests against recorded fixtures pass; one gateway request per invoice under retries.
- **Risks:** high (external contract).

### T-18 — Payment webhook and reconciliation · Size: M · Role: backend-engineer
- **Goal:** webhook endpoint with token check, idempotent processing, settlement and divergence flag.
- **Covers:** AC-25, AC-26, AC-27
- **Module:** `reconciliation`, `invoicing`
- **Likely files:** `.../reconciliation/**` (create), `.../invoicing/**` (edit), `db/migration/V11__reconciliation.sql` (create)
- **Depends on:** T-17
- **Parallel with:** —
- **Done when:** tests for duplicate webhooks, invalid token and amount divergence pass.
- **Risks:** high (money). Recovery: payments stored raw before applying.

### T-19 — Overdue job · Size: S · Role: backend-engineer
- **Goal:** daily job marks issued, unpaid, past-due invoices as `overdue`.
- **Covers:** AC-28
- **Module:** `invoicing`
- **Likely files:** `.../invoicing/**` (edit)
- **Depends on:** T-17
- **Parallel with:** T-18 only if no shared files (likely shared: run after)
- **Done when:** test with a fixed `Clock` passes.
- **Risks:** low.

### T-20 — Cancellation before payment · Size: M · Role: backend-engineer
- **Goal:** operator cancels an issued unpaid invoice; cancels charge and NFS-e via the outbox; 409 when paid.
- **Covers:** AC-29
- **Module:** `invoicing`, `fiscal`, `payments`
- **Likely files:** edits in the three modules
- **Depends on:** T-18
- **Parallel with:** —
- **Done when:** cancellation and 409 tests pass.
- **Risks:** medium (compensation across modules).

### T-21 — Invoice and fiscal document queries · Size: S · Role: backend-engineer
- **Goal:** list and get endpoints with cursor pagination, tenant-scoped.
- **Covers:** AC-31, AC-03
- **Module:** `invoicing`, `fiscal`
- **Likely files:** controllers in both modules (edit)
- **Depends on:** T-15
- **Parallel with:** T-17–T-20 if controllers stay in separate files
- **Done when:** pagination and cross-tenant 404 tests pass.
- **Risks:** low.

### T-22 — Simulator CLI and scenario test · Size: M · Role: backend-engineer
- **Goal:** `simulator` Gradle subproject that provisions a sample tenant, sends a month of events (with duplicates and late events), advances the clock, and prints invoice, NFS-e and charge.
- **Covers:** AC-30, success metrics 1–2
- **Module:** `simulator` (new subproject)
- **Likely files:** `simulator/**` (create), `settings.gradle.kts` (edit), `src/test/.../MonthScenarioIT.java` (create)
- **Depends on:** T-18
- **Parallel with:** T-19–T-21
- **Done when:** scenario test passes and the CLI exits 0 on a clean clone.
- **Risks:** medium (test clock control across processes).

### T-23 — Observability · Size: M · Role: backend-engineer
- **Goal:** spans across the flow, business metrics, a provisioned Grafana dashboard, personal data masked in traces.
- **Covers:** NFR observability, NFR privacy
- **Module:** cross-cutting
- **Likely files:** `observability/grafana/**` (create), `compose.yaml` (edit), small edits in modules
- **Depends on:** T-18
- **Parallel with:** T-22 (if `compose.yaml` is only edited here)
- **Done when:** one trace links ingestion to reconciliation for a simulated invoice; dashboard shows invoices by state.
- **Risks:** low.

### T-24 — Release readiness for v0.1.0 · Size: S · Role: qa-engineer
- **Goal:** run `release-readiness`, update README with demo GIF and links to ADRs, check NFR targets.
- **Covers:** all (verification), success metrics
- **Module:** —
- **Likely files:** `README.md` (edit), `docs/` (edit)
- **Depends on:** T-01–T-23
- **Parallel with:** —
- **Done when:** verdict GO or GO with accepted caveats; tag `v0.1.0` pushed with approval.
- **Risks:** none.

## Coverage matrix
| AC | Tasks |
|---|---|
| AC-01, AC-02 | T-05 (T-03 for error format) |
| AC-03 | T-05, T-21 |
| AC-04 | T-06 |
| AC-05, AC-06, AC-07, AC-08 | T-09 (T-02 for tier convention, T-03 for rounding) |
| AC-09 | T-10 |
| AC-10, AC-11, AC-12 | T-07 |
| AC-13, AC-14, AC-16 | T-08 |
| AC-15 | T-12 |
| AC-17, AC-18 | T-11 |
| AC-19, AC-20 | T-13 |
| AC-21, AC-22 | T-15 (T-14 for retries) |
| AC-23 | T-16 |
| AC-24 | T-17 (T-14 for repeat safety) |
| AC-25, AC-26, AC-27 | T-18 |
| AC-28 | T-19 |
| AC-29 | T-20 |
| AC-30 | T-22 |
| AC-31 | T-21 |

## Decisions taken at approval
- **`accounts` module:** owns tenants, API keys, the operator key and customers.
- **`shared` module:** open module for `Money`, `BillingPeriod`, error handling and the outbox.
- **F-22:** covered by the new AC-31 in PRD v1.1.
- **Dependencies:** Spring Security, Testcontainers modules, Resilience4j and WireMock approved up front for the tasks that need them.

## Cross-cutting risks
- **Asaas sandbox account** for T-04 and T-17: created by the owner; credentials only in a local `.env`.
- **Close vs. continuous aggregate lag:** settled, the close reads raw events by `billing_period` (ADR 0004, amendment 1).
- **Recovery for high-risk tasks:** every migration is forward-only; a bad release is fixed by a new migration, never by editing an applied one.
- **Build verification in the cloud session:** needs the Maven, Gradle, toolchain and Docker registry domains allowed; until then CI is the only place the build runs.
