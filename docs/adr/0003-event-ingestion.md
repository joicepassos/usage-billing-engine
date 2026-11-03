# ADR 0003 — Event ingestion and idempotency
Status: accepted · Date: 2026-11-02

## Context
Tenants send usage events over HTTP and retry on timeouts. Some events arrive late, and some reference customers not registered yet. Losing or double counting an event changes what a customer pays.

## Options considered
1. **Queue in front** (Kafka, Redis Streams). Absorbs spikes; adds infrastructure the MVP volume does not need.
2. **Direct insert per request**, batched, with a uniqueness constraint as the idempotency guard.

## Decision
- `POST /v1/events` accepts batches of up to 500 events and inserts them directly into a TimescaleDB hypertable, answering 202 after the transaction commits.
- Idempotency key: unique `(tenant_id, transaction_id)` plus a hash of the payload. Same id and same payload: 200 with the original result. Same id, different payload: that event gets 409; the rest of the batch is processed.
- Late events: an event timestamped in period P counts for P if it arrives before P's grace window ends (1 hour after the period end). After that, it is assigned to the next open period and keeps its original timestamp.
- Unknown customers: events are stored with the external customer id; usage is billed at the first close after the customer is registered, and every close logs an alert with the count of unbilled events.

## Consequences
- Positive: no extra infrastructure; the database constraint is the single source of truth for idempotency.
- Negative: ingestion throughput is bounded by the database.
- Risks: write contention at high volume. Mitigation: batch inserts; revisit with a queue when sustained load exceeds what one database handles.

## How to revert
Put a queue in front of the same insert path; the idempotency constraint stays.
