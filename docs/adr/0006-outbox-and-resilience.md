# ADR 0006 — External calls through an outbox with Resilience4j
Status: accepted · Date: 2026-11-02

## Context
The fiscal issuer and the payment gateway are remote, can be slow or unavailable, and their operations create real-world side effects that must happen exactly once.

## Options considered
1. **Synchronous calls inside the business transaction.** Simple; an outage blocks the flow and a timeout leaves state unknown.
2. **Modulith events only.** Good for internal reactions; no control over attempts, backoff or per-operation state.
3. **Dedicated outbox table plus a worker.**

## Decision
Internal reactions use Modulith events. Every external call is written to an `outbox` table in the same transaction as the state change, then executed by a worker with Resilience4j: timeout, retry with exponential backoff only for idempotent operations, and a circuit breaker per external system. Each outbox entry carries an idempotency key sent to the provider when it supports one. External calls never run inside a database transaction.

## Consequences
- Positive: outages do not block closes or lose requests; attempts are visible and auditable.
- Negative: eventual consistency between invoice state and provider state.
- Risks: duplicate side effects after a crash between the call and the outbox update. Mitigation: provider idempotency keys, and lookups before retrying non-idempotent operations.

## How to revert
Not expected; providers could be called synchronously from the worker without the table, losing auditability.
