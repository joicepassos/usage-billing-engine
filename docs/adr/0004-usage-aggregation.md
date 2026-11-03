# ADR 0004 — Usage aggregation with TimescaleDB
Status: accepted · Date: 2026-11-02

## Context
Operators and tenants need current-period usage at any time, and the cycle close must sum a whole month of events per customer and metric.

## Options considered
1. **Aggregate only at close.** Simple; no live usage view and an expensive close.
2. **Application-maintained counters.** Fast reads; double-counting risk on retries and late events.
3. **TimescaleDB continuous aggregates** over the event hypertable.

## Decision
Option 3. A continuous aggregate keeps daily usage per tenant, customer and metric for the usage query (freshness target: 1 minute). The cycle close does not trust aggregate freshness: it reads raw events for the closing period, or forces a refresh of the closing window first. The exact approach is settled in T-08/T-11 and recorded as an amendment to this ADR.

## Consequences
- Positive: cheap live usage queries; aggregation logic stays in the database.
- Negative: ties the storage layer to TimescaleDB.
- Risks: refresh lag causing missed events at close. Mitigation: the close never depends on the aggregate's lag.

## How to revert
Drop the continuous aggregate and query raw events; the close already supports it.
