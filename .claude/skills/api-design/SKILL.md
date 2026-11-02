---
name: api-design
description: Designs and reviews REST APIs in Spring Boot. Typed contracts with records and Bean Validation, API key authentication, tenant-scoped authorization, RFC 9457 errors, pagination, idempotency, webhooks and compatibility, documented code-first with springdoc. Use when asked to "create an endpoint", "design the API", "webhook", "contract", "cria o endpoint", or before exposing data to a client.
---

# API Design

Predictable contracts, secure by default and easy to evolve without breaking clients.

## 1. Before designing
- Map existing controllers and follow the project convention (paths, error format, authentication).
- List consumers (startup backends sending events, operators, payment gateway webhooks). Changing a public contract requires compatibility.

## 2. Resources and methods
| Action | Method | Route | Success |
|---|---|---|---|
| List | GET | `/v1/invoices?cursor=&limit=` | 200 |
| Get | GET | `/v1/invoices/{id}` | 200 / 404 |
| Create | POST | `/v1/customers` | 201 + `Location` |
| Ingest batch | POST | `/v1/events` | 202 |
| Partial update | PATCH | `/v1/customers/{id}` | 200 |
| Non-CRUD action | POST | `/v1/invoices/{id}/finalize` | 200 |
Plural nouns, kebab-case, no verbs in the path except non-CRUD actions. GET never changes state.

## 3. Typed contract
- Request and response are Java records with Bean Validation (`@NotNull`, `@Size`, `@Positive`, custom validators). Validate with `@Valid` on the controller.
- Reject unknown properties on writes (`FAIL_ON_UNKNOWN_PROPERTIES` for request DTOs) to avoid mass assignment.
- Never expose JPA entities. Map to response records (no internal ids of other tenants, no API key hashes, no audit internals).
- Document with springdoc annotations (`@Operation`, `@ApiResponse`, `@Schema`); the generated OpenAPI is the published contract and is reviewed in PRs.

## 4. Errors — one format (RFC 9457)
```json
{ "type": "https://usage-billing-engine/errors/validation", "title": "Invalid request", "status": 422,
  "detail": "2 fields are invalid", "errors": { "quantity": ["must be positive"] }, "requestId": "..." }
```
| Situation | Status |
|---|---|
| Malformed body | 400 |
| Validation failure | 422 |
| Missing or invalid API key | 401 |
| Resource of another tenant | 404 (never reveal existence) |
| Not found | 404 |
| Same idempotency key, different payload | 409 |
| Rate limit | 429 + `Retry-After` |
| Unexpected failure | 500 with a generic message; details only in the log with `requestId` |

## 5. Security
- API key in `Authorization: Bearer sk_test_...`; stored as a hash; resolved to a tenant by a filter before the controller.
- Every query is tenant-scoped through `@TenantId`; never take `tenantId` from the body or path.
- Parameterized queries / JPA only.
- Rate limit ingestion and any expensive endpoint.
- Incoming webhooks: verify the provider's authentication (signature or shared token), respond fast, process asynchronously and idempotently.

## 6. Pagination, filters and ordering
Cursor-based for growing lists (`?cursor=&limit=`, response with `nextCursor`); `limit` capped (e.g. 100). Filters and `sort` from an allowlist of fields.

## 7. Idempotency and concurrency
- Events: `(tenant_id, transaction_id)` is unique. Repeat with same payload: 200 with the original result. Same id, different payload: 409.
- Other POSTs that create money-relevant state (customers, invoice actions): accept `Idempotency-Key` and return the same response on repeats.
- Concurrent updates: optimistic locking (`@Version`) and 409.

## 8. Evolution and compatibility
- Adding an optional field is compatible. Removing or renaming a field, changing a type or making a field required is **breaking**: new version (`/v2`) or a deprecation period.
- Breaking changes need an ADR.

## 9. Observability
Structured log per request (route, status, duration, `requestId`, tenant id) without sensitive data. Trace spans across ingestion → aggregation → invoicing → fiscal → payments.

## Output
Endpoint table, request/response records, authorization rules, error cases and a test plan. When implementing: tests for success, validation, 401, cross-tenant 404, idempotent repeat and 409 (PASSED/FAILED/NOT_RUN).

## Checklist
- [ ] Every endpoint validates input with Bean Validation.
- [ ] Every query is tenant-scoped; cross-tenant access returns 404.
- [ ] Responses expose no internal fields or stack traces.
- [ ] Errors use Problem Details with `requestId`.
- [ ] Lists paginated with a cap.
- [ ] No breaking change without versioning and an ADR.
