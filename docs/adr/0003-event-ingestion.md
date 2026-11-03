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

## Amendment 1 — 2026-11-03: idempotency ledger, period at ingestion, batch response
- **Idempotency ledger.** TimescaleDB requires unique indexes on a hypertable to include the partitioning column (`occurred_at`), so `(tenant_id, transaction_id)` cannot be unique on `usage_event`. A regular table, `ingested_transaction`, with that primary key records every accepted event (payload hash, assigned period) and is written in the same transaction as the event. `INSERT ... ON CONFLICT DO NOTHING` on it decides whether an event is new.
- **Billing period assigned at ingestion.** Starting from the period that contains `occurred_at`, the event moves to the next period while its arrival time is past that period's grace end. The result is stored in `usage_event.billing_period` and never recomputed. Events more than 5 minutes in the future are rejected.
- **Batch response.** Schema errors (malformed JSON, missing fields, more than 500 events) reject the whole batch with 422. Business outcomes are reported per event: `accepted`, `duplicate` (same payload, original result returned), `conflict` (same transaction id, different payload, status 409) or `rejected` (unknown metric, missing `unique_key` for a unique metric, future timestamp, status 422). The response is 202 when at least one event was accepted and 200 when none was.
