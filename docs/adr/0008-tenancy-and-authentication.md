# ADR 0008 — Tenancy and API key authentication
Status: accepted · Date: 2026-11-02

## Context
Every tenant's data must be invisible to other tenants. Clients are backend services, not people; operators need a few privileged actions.

## Options considered
1. **OAuth2 client credentials with Keycloak.** Standard; an extra service with no human users in the MVP.
2. **API keys per tenant.** Industry norm for billing APIs; simple to integrate.
3. **Row-level security in Postgres** for isolation. Strong; harder to test and reason about at this stage.

## Decision
- Tenants authenticate with an API key (`sk_test_` prefix) sent as a bearer token. Keys are random, shown once, stored as a hash and compared in constant time. Operator actions require a separate operator key.
- The `accounts` module owns tenants, keys and customers.
- Isolation uses Hibernate's `@TenantId` on every tenant-owned entity, with the tenant resolved from the key before the controller. An architecture test fails when a tenant-owned entity lacks `@TenantId`. Cross-tenant access returns 404.
- Errors follow RFC 9457 Problem Details with a `requestId`.

## Consequences
- Positive: simple client integration; isolation enforced in one place.
- Negative: isolation lives in the application layer only.
- Risks: a native query bypassing the tenant filter. Mitigation: no native queries on tenant data without review; RLS reconsidered in a future ADR.

## How to revert
Add OAuth2 alongside keys when a portal with human users exists.
