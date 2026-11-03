# Architecture decision records

| ADR | Decision | Status |
|---|---|---|
| [0001](0001-modular-monolith.md) | Modular monolith with Spring Modulith | accepted |
| [0002](0002-money-and-time.md) | Money and time representation | accepted |
| [0003](0003-event-ingestion.md) | Event ingestion and idempotency | accepted |
| [0004](0004-usage-aggregation.md) | Usage aggregation with TimescaleDB | accepted |
| [0005](0005-billing-cycle-and-invoice-lifecycle.md) | Billing cycle and invoice lifecycle | accepted |
| [0006](0006-outbox-and-resilience.md) | External calls through an outbox with Resilience4j | accepted |
| [0007](0007-external-system-ports.md) | Fiscal issuer and payment gateway behind ports | accepted |
| [0008](0008-tenancy-and-authentication.md) | Tenancy and API key authentication | accepted |
| [0009](0009-tier-boundaries.md) | Tier boundary convention | accepted |

New decisions use the template in `.claude/agents/architect.md`. Superseded ADRs stay in place with their status updated.
