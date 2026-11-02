# PRD — usage-billing-engine MVP
Version: 1.1 · Status: approved · Date: 2026-11-02

## 1. Problem
Brazilian startups that charge by consumption (API calls, minutes, tokens) end up building billing by hand: counting events in their own database, computing invoices in a cron job, issuing the NFS-e in a municipal portal and creating Pix or boleto charges somewhere else. International billing tools handle metering and pricing but not the Brazilian fiscal document or local payment methods, so the tenant still glues four systems together and reconciles them in spreadsheets.

## 2. Actors
| Actor | Context | Main pain | Expected gain |
|---|---|---|---|
| Tenant backend (API client) | A startup's service that sends usage and reads invoices | Must build idempotent metering, pricing and invoicing itself | Send events, get invoices, NFS-e and charges out |
| Operator | Person running the engine for a tenant (in the MVP, the project owner) | No visibility or control over what will be billed | Inspect usage, hold or finalize invoices, fix fiscal rejections |
| End customer | The tenant's client who pays | Receives inconsistent invoices and documents | Correct amount, NFS-e and a payable charge |
| Portfolio reader | Recruiter or engineer reading the repository | Hard to judge depth from a toy project | Runs a full month locally and reads the decisions behind it |

## 3. Goals and success metrics
| Goal | Metric | Target | How measured |
|---|---|---|---|
| End-to-end billing works | Simulated month produces invoice, authorized NFS-e and charge per customer | 100% of simulated customers | Scenario test using the simulator |
| No usage is lost or double counted | Difference between events sent and usage billed | 0 | Scenario test with duplicates and late events |
| Amounts are correct | Invoice totals vs. independently computed expected totals | Exact match (`compareTo == 0`) | Property-based tests on pricing |
| Easy to run | Time from clean clone to first invoice printed | < 10 min `[ASSUMPTION]` | Manual run following the README |
| Credible engineering | CI green on main; module verification passing | Always | GitHub Actions |

## 4. Main journeys
- **J1 — Set up billing:** operator provisions a tenant and API key → tenant registers billable metrics, a plan with charges, customers with fiscal data and subscriptions.
- **J2 — Report usage:** tenant sends batches of usage events → engine stores them idempotently → tenant or operator queries current-period usage.
- **J3 — Bill and collect:** period ends → grace window passes → cycle close drafts invoices → invoices finalize → NFS-e issued → charge created in the gateway → payment webhook arrives → invoice settled. Failure paths: fiscal rejection, overdue, cancellation before payment.

## 5. MVP scope
| ID | Feature | Priority | Journey |
|---|---|---|---|
| F-01 | Tenant provisioning with API key (operator command or seed, no self-service) | Must | J1 |
| F-02 | Customer registration with fiscal data (CPF/CNPJ, name, address, email) | Must | J1 |
| F-03 | Billable metrics with aggregation `sum`, `count`, `max`, `unique` | Must | J1 |
| F-04 | Plans with charges: per unit, graduated tiers, package, monthly minimum | Must | J1 |
| F-05 | Subscriptions on calendar months | Must | J1 |
| F-06 | Plan change scheduled for the next period | Should | J1 |
| F-07 | Batch event ingestion, idempotent by transaction id | Must | J2 |
| F-08 | Late events within the grace window; later ones roll into the next period | Must | J2 |
| F-09 | Events for unknown customers kept and billed once the customer exists | Must | J2 |
| F-10 | Current-period usage query per customer and metric | Should | J2 |
| F-11 | Cycle close: drafts invoices from aggregated usage | Must | J3 |
| F-12 | Automatic finalization after the grace window | Must | J3 |
| F-13 | Operator actions: finalize early, hold, release | Should | J3 |
| F-14 | NFS-e issuance through the fiscal issuer port, with IBS/CBS rate table | Must | J3 |
| F-15 | Local fiscal issuer simulator (authorize and reject) as default | Must | J3 |
| F-16 | Fiscal rejection handling: state, alert, fix and reissue | Must | J3 |
| F-17 | Real national-standard NFS-e adapter behind a profile | Could | J3 |
| F-18 | Payment charge creation (Pix and boleto) in the Asaas sandbox | Must | J3 |
| F-19 | Payment webhook and reconciliation | Must | J3 |
| F-20 | Overdue marking by a daily job | Should | J3 |
| F-21 | Invoice cancellation before payment (cancels charge and NFS-e) | Should | J3 |
| F-22 | Invoice and fiscal document queries | Must | J3 |
| F-23 | Simulator CLI: one month for a sample tenant, prints invoice, NFS-e and charge | Must | J1–J3 |
| F-24 | Traces across the flow and a Grafana dashboard with business metrics | Should | — |

## 6. Out of scope (now)
- Customer portal or any UI (the simulator and API docs are the interface).
- Self-service tenant onboarding and per-tenant digital certificates.
- Volume tiers, prepaid credits, proration and mid-period plan changes.
- Refunds, credit notes and cancellation after payment.
- Pix Automático and card payments.
- Interest, fines and dunning sequences for overdue invoices.
- Currencies other than BRL; billing periods other than calendar months.
- Real (non-sandbox) payment or fiscal endpoints in the default setup.

## 7. Acceptance criteria

**Setup**
- AC-01 (F-01): Given an operator command, when a tenant is provisioned, then an API key with the `sk_test_` prefix is shown once and only its hash is stored.
- AC-02 (F-01): Given a request without a valid API key, when any `/v1` endpoint is called, then the response is 401 in Problem Details format.
- AC-03 (F-01): Given tenants A and B, when A requests a resource id owned by B, then the response is 404.
- AC-04 (F-02): Given a customer without a valid CPF/CNPJ or address, when it is created, then the response is 422 listing the invalid fields.
- AC-05 (F-04): Given a graduated charge with tiers 0–1,000 at R$ 0.010, 1,001–10,000 at R$ 0.008 and above at R$ 0.005, when 12,500 units are billed, then the line amount is R$ 94.50. `[TO CONFIRM tier boundary convention]`
- AC-06 (F-04): Given a package charge of R$ 25.00 per 1,000 units, when 2,001 units are billed, then the line amount is R$ 75.00.
- AC-07 (F-04): Given a monthly minimum of R$ 100.00, when usage amounts to R$ 60.00, then the invoice includes a minimum-adjustment line of R$ 40.00.
- AC-08 (F-04): For any generated usage and tier set, the invoice total equals the sum of its lines, and each line is rounded once with HALF_EVEN to 2 decimals.
- AC-09 (F-06): Given a plan change requested on day 15, when the current period closes, then it is billed on the old plan and the next period uses the new one.

**Usage**
- AC-10 (F-07): Given a batch of up to 500 valid events, when it is posted, then all are persisted and the response is 202 with the count accepted.
- AC-11 (F-07): Given an event whose transaction id was already accepted with the same payload, when it is posted again, then the response is 200 with the original result and usage does not change.
- AC-12 (F-07): Given an event whose transaction id was already accepted with a different payload, when it is posted, then that event is rejected with 409 and the rest of the batch is processed.
- AC-13 (F-08): Given an event timestamped in period P that arrives within the grace window after P ends, when P closes, then it is billed in P.
- AC-14 (F-08): Given an event for period P that arrives after P was closed, when the next period closes, then it is billed there and keeps its original timestamp.
- AC-15 (F-09): Given events for an unknown customer, when the period closes, then no invoice is created for it, an alert is logged with the count, and after the customer is registered the usage is billed at the next close.
- AC-16 (F-10): Given events already accepted in the current period, when usage is queried, then the aggregated value reflects events accepted up to 1 minute earlier `[ASSUMPTION]`.

**Bill and collect**
- AC-17 (F-11): Given subscriptions with usage, when the period ends in `America/Sao_Paulo` and the grace window passes, then exactly one draft invoice per subscription is created, even if the close job runs twice.
- AC-18 (F-11): Given a subscription with zero usage and no minimum, when the period closes, then no invoice is created.
- AC-19 (F-12): Given a draft invoice not on hold, when the grace window ends, then it becomes `finalized` and NFS-e issuance is requested.
- AC-20 (F-13): Given a draft invoice on hold, when the grace window ends, then it stays `on_hold` until released or finalized by the operator.
- AC-21 (F-14): Given a finalized invoice, when the NFS-e is authorized, then the fiscal document stores the number, verification code and IBS/CBS values from the rate valid on the issue date.
- AC-22 (F-14, F-15): Given the fiscal issuer times out or returns 5xx, when issuance is retried with backoff, then the invoice stays `finalized` and is issued once the issuer recovers, with no duplicate NFS-e.
- AC-23 (F-16): Given a business rejection, when the issuer answers, then the invoice becomes `fiscal_rejected`, no charge is created, and after the operator fixes customer data and reissues, the flow continues.
- AC-24 (F-18): Given an authorized NFS-e, when the charge is created, then the gateway receives one request per invoice (repeat-safe) and the invoice becomes `issued` with Pix and boleto payment data and a due date 5 days later by default.
- AC-25 (F-19): Given a payment confirmation webhook with a valid token, when it is received once or several times, then the invoice becomes `paid` exactly once.
- AC-26 (F-19): Given a webhook with an invalid token, when it is received, then it is rejected with 401 and nothing changes.
- AC-27 (F-19): Given a confirmed payment with a different amount than the invoice, when reconciled, then the invoice is flagged as divergent and not marked paid.
- AC-28 (F-20): Given an issued invoice past its due date without payment, when the daily job runs, then it becomes `overdue`.
- AC-29 (F-21): Given an issued, unpaid invoice, when the operator cancels it, then the charge and the NFS-e are cancelled and the invoice becomes `cancelled`; cancelling a paid invoice returns 409.
- AC-31 (F-22): Given invoices of a tenant, when they are listed with `limit=2`, then two are returned with a `nextCursor`, and invoices of other tenants never appear.
- AC-30 (F-23): Given a clean clone with Docker running, when the simulator runs one month for the sample tenant, then it prints, per customer, the invoice lines, the NFS-e number and the charge id, and exits 0.

## 8. Non-functional requirements
- **Performance:** a 500-event batch acknowledged in < 500 ms p95 on the local stack; cycle close for 1,000 subscriptions in < 60 s `[ASSUMPTION]`.
- **Reliability:** no lost or double-counted events; external calls retried with backoff and protected by a circuit breaker; every external side effect idempotent.
- **Security:** API key auth on all `/v1` routes; tenant isolation on every query; webhook authentication; no secrets in the repository.
- **Privacy/LGPD:** customer CPF/CNPJ, name, address and email are processed only to issue NFS-e and charges; never logged; masked in traces. Retention follows fiscal record-keeping rules `[TO CONFIRM retention period]`.
- **Observability:** one trace from ingestion to reconciliation for a given invoice; metrics for events ingested, invoices by state, issuer and gateway failures.
- **Reproducibility:** the whole demo runs locally with simulators and no external accounts; CI runs `./gradlew check` on every push.

## 9. Assumptions
- [ASSUMPTION] Grace window of 1 hour after the period ends.
- [ASSUMPTION] Batch size limit of 500 events.
- [ASSUMPTION] A single fiscal provider configuration (one service code and ISS rate) for all tenants in the MVP.
- [ASSUMPTION] Operator actions are API endpoints protected by a separate operator key; no UI.
- [ASSUMPTION] Flyway for database migrations.

## 10. Risks and open questions
| Risk/question | Impact | Mitigation/owner |
|---|---|---|
| National NFS-e API details (DPS layout, error codes) differ from the simulator | Real adapter needs rework | Model the simulator on published documentation; keep the real adapter as Could · architect |
| Asaas sandbox behavior and webhook authentication | Reconciliation tests may not match reality | Contract tests against recorded sandbox responses · backend-engineer |
| Continuous aggregate refresh lag at period close | Close may miss recent events | Close reads raw events for the closing period or forces a refresh · architect |
| Tier boundary convention (inclusive/exclusive) | Wrong amounts | Decide and document in `CONTEXT.md` before pricing work · product-manager |
| Fiscal data retention period | LGPD and fiscal compliance | Confirm with a reference before implementing deletion · owner |
