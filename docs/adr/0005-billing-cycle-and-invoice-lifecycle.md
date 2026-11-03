# ADR 0005 — Billing cycle and invoice lifecycle
Status: accepted · Date: 2026-11-02

## Context
The engine must turn a month of usage into an invoice, a Brazilian service invoice (NFS-e) and a payment charge, keeping fiscal and financial state consistent.

## Decision
- **Cycle:** calendar month in `America/Sao_Paulo` for every subscription; no proration. Plan changes apply from the next period.
- **Close:** a scheduled job runs after the grace window and drafts one invoice per subscription and period (unique constraint), only when there is usage or a monthly minimum.
- **Finalization:** drafts finalize automatically when the grace window ends unless on hold; operators can finalize early, hold or release.
- **NFS-e:** requested at finalization. Technical failures are retried; a business rejection moves the invoice to `fiscal_rejected` until the operator fixes the data and reissues.
- **Charge:** created only after the NFS-e is authorized. Due date defaults to 5 days after issue (per-tenant setting).
- **Settlement:** a confirmed payment with the exact amount marks the invoice `paid`; a different amount flags it as divergent.
- **Overdue and cancellation:** a daily job marks unpaid invoices past due as `overdue`. Cancellation is allowed only before payment and cancels the charge and the NFS-e. Refunds and credit notes are out of scope.

States: `draft`, `on_hold`, `finalized`, `fiscal_rejected`, `issued`, `paid`, `overdue`, `cancelled`.

## Consequences
- Positive: fiscal and financial records never disagree; every transition is driven by a domain event.
- Negative: payment is delayed by NFS-e issuance time.
- Risks: issuer outages blocking charges. Mitigation: retries through the outbox; invoices wait in `finalized`.

## How to revert
Issuing the NFS-e on payment instead of finalization is a policy change in the fiscal module triggered by a different event; it needs its own ADR.
