---
name: security-audit
description: Reviews backend security (API, API key auth, tenant isolation, data, secrets, dependencies, webhooks, integrations, LGPD) based on the OWASP Top 10, classifying findings by severity with file:line evidence and a suggested fix. Use before a release, when touching authentication, payments, fiscal data, webhooks or personal data, or when asked for "security review", "is this safe?", "audita a segurança".
---

# Security Audit

Finds real, exploitable vulnerabilities with evidence. Prefer a few confirmed findings to a long list of guesses.

## Scope
Define first: current diff, one feature or the whole project. For the whole project, start with the highest-risk surfaces: API key handling, ingestion endpoint, webhook endpoint, tenant isolation, outbox workers, configuration.

## Procedure

### 1. Map the surface
- Inputs: REST endpoints, webhook endpoint, scheduled jobs, simulator, configuration properties.
- Assets: API keys, customer personal data (CPF, CNPJ, addresses, emails), invoices and amounts, other tenants' data, certificates (real issuer profile only).
- Trust boundaries: client ↔ API ↔ database ↔ fiscal issuer ↔ payment gateway.

### 2. Check by category

**Access control (A01) — the most common**
- [ ] Every endpoint except health/docs requires a valid API key, checked in the security filter chain.
- [ ] Tenant comes only from the authenticated key; every entity uses `@TenantId`; cross-tenant ids return 404 (no IDOR).
- [ ] Operator-only actions (finalize, hold, cancel) are not reachable with a client key if roles differ.

**Injection (A03)**
- [ ] JPA / parameterized queries only; no string-built native SQL with input.
- [ ] No SpEL or expression evaluation of user input; no `Runtime.exec`.
- [ ] Jackson configured without default typing on untrusted input.

**Authentication (A07)**
- [ ] API keys random and long enough, stored as a hash (not reversible), compared in constant time, revocable.
- [ ] Keys never logged; only the prefix shows in logs and UI.
- [ ] Rate limit on ingestion and on repeated authentication failures.

**Secrets and configuration (A02/A05)**
- [ ] No key, token, certificate or password in code, `application*.yml`, history or logs (`git grep -niE "(api[_-]?key|secret|token|password)\s*[:=]"`).
- [ ] `.env*` in `.gitignore`; `.env.example` without real values.
- [ ] Actuator exposes only health/info/metrics needed; no `env`, `heapdump` or `configprops` publicly.
- [ ] Stack traces off in error responses; springdoc UI acceptable only in local profile.

**Data exposure and LGPD**
- [ ] Responses return only needed fields (DTO records).
- [ ] Logs and traces without CPF/CNPJ, addresses, API keys; personal data minimized and masked.
- [ ] Personal data has a purpose, retention and a way to delete or anonymize.

**Integrations and webhooks (A08)**
- [ ] Incoming webhooks authenticated (provider signature or shared token), replay-safe and idempotent.
- [ ] Outbound calls only to configured base URLs; real (non-sandbox) endpoints only behind an explicit profile.
- [ ] TLS verification never disabled.

**Dependencies and supply chain (A06)**
- [ ] Dependency versions from the version catalog; Gradle wrapper checksum validated; report critical/high CVEs with the path. Do not upgrade without approval.
- [ ] Unknown or newly created artifacts questioned.

**SSRF (A10)**
- [ ] No endpoint fetches a URL supplied by the client.

### 3. Classify
| Severity | Criterion |
|---|---|
| Critical | Unauthenticated remote exploitation leads to mass data leak, code execution or account takeover |
| High | Access to another tenant's data or actions; exposed secret; billing or payment bypass |
| Medium | Requires a specific condition or an authenticated client; limited impact |
| Low | Hardening / defense in depth |

Only report as confirmed what has **evidence** (file:line + exploit scenario). The rest goes under "to verify".

## Report template
```markdown
# Security review — <scope> — YYYY-MM-DD
Summary: X critical, Y high, Z medium, W low. Blocks release? yes/no.

## [HIGH] Cross-tenant read in GET /v1/invoices/{id}
- **Where:** `invoicing/internal/InvoiceController.java:41`
- **Scenario:** tenant A changes the id and reads tenant B's invoice.
- **Fix:** query through the tenant-scoped repository; return 404.
- **Verification:** integration test expecting 404 for another tenant's invoice.

## To verify (insufficient evidence)
## Out of scope / not verified (NOT_RUN)
```

## Rules
- Do not probe third-party systems or run scanners against real endpoints without explicit authorization.
- Never copy secrets into the report; cite the file and mask (`sk_live_****`). Recommend **rotating** any secret that reached Git.
- Critical/high fixes: propose a minimal patch and a test; apply only with approval if it changes behavior.
