# Project context

- **Product / goal:** usage-based billing engine for startups that charge by consumption. Events in, monthly cycle closes, invoice out, NFS-e issued, Pix/boleto charge created, payment reconciled.
- **Audience:** public portfolio project (phase 1). A private product may build on it later (phase 2).
- **Example scenario:** a fictional transcription API that charges per processed minute.
- **Repository:** `usage-billing-engine`, MIT license. Code, commits, docs and PRs in English.
- **Base package:** `io.github.joicepassos.billing` (modules are sub-packages)

## Stack
- Java 25 (toolchain), Spring Boot 4.1, Spring Modulith 2.1
- Gradle with Kotlin DSL and version catalog (`gradle/libs.versions.toml`)
- PostgreSQL + TimescaleDB, Resilience4j, OpenTelemetry, Grafana
- API is code-first; springdoc generates the OpenAPI document
- Migrations: Flyway `[ASSUMPTION]`
- Tests: JUnit 5, AssertJ, jqwik, Testcontainers, WireMock, Modulith `ApplicationModules.verify()`

## Modules
accounts · metering · pricing · invoicing · fiscal · payments · reconciliation, plus an open `shared` module (money, periods, errors, outbox) and a `simulator` CLI subproject for the demo

## Approved decisions
| Topic | Decision |
|---|---|
| Architecture | Modular monolith (Spring Modulith) |
| Module communication | Modulith application events with the Event Publication Registry; a dedicated `outbox` table only for external calls |
| Ingestion | Direct insert into a TimescaleDB hypertable; batch endpoint; 202 after persisting |
| Duplicate event | Same `(tenant_id, transaction_id)`: 200 with the original result; different payload with same id: 409 |
| Late event | Short grace window after period end, then counted in the next cycle with its original timestamp; never rejected |
| Unknown customer | Event accepted and stored; billed once the customer is registered; alert at cycle close |
| Aggregation | TimescaleDB continuous aggregates |
| Billing cycle | Calendar month, no proration; plan changes apply next cycle |
| Pricing models | Per unit, graduated tiers, package, monthly minimum (Strategy per model). Volume tiers out of scope |
| Money | `BigDecimal`; unit prices with higher scale, HALF_EVEN rounding on invoice lines; BRL only |
| Time | Events stored in UTC; cycles close in `America/Sao_Paulo` |
| Invoice finalization | Automatic after the grace window; endpoints to finalize early or hold |
| NFS-e timing | Issued at invoice finalization |
| NFS-e rejection | Technical errors retried; business rejection sets `fiscal_rejected`, raises an alert, allows fix and reissue. Charge only after the NFS-e is authorized |
| Fiscal issuer | `FiscalIssuer` port: local simulator by default (WireMock), real national-standard adapter behind a profile. No certificate or CNPJ in the repo |
| IBS/CBS | Rate table with validity dates (test rates 0.9% CBS, 0.1% IBS for 2026) |
| External calls | Async via outbox; Resilience4j timeout, retry with backoff, circuit breaker |
| Payment gateway | Asaas sandbox behind a `PaymentGateway` port |
| Due date | Per-tenant setting, default 5 days after issue; daily job marks overdue invoices |
| Cancellation | Only before payment; cancels charge and NFS-e. Refunds and credit notes out of scope |
| Auth | API key per tenant (`sk_test_...` prefix, hash stored) |
| Tenant isolation | Hibernate `@TenantId` plus an architecture test; Postgres RLS as a future ADR |
| Observability | OpenTelemetry traces end to end; Grafana dashboard with business metrics |
| Demo | CLI simulator that generates a month of events and prints invoice, NFS-e and charge |

## Out of scope (MVP)
Customer portal, Pix Automático, prepaid credits, proration, refunds, credit notes, per-tenant certificates, self-service onboarding, admin UI.

## Restrictions
- Never commit certificates, CNPJ/CPF of real people or companies, API keys or `.env` files.
- Never call real (non-sandbox) payment or fiscal endpoints.

## Known commands
- `./gradlew check` — tests and static checks (per-task gate)
- `./gradlew build` — full build
- `docker compose up` — Postgres/TimescaleDB and Grafana LGTM (`./gradlew bootRun` starts it automatically through Spring Boot Docker Compose support)
- `./gradlew wrapper --gradle-version <x>` — upgrade Gradle (wrapper on 9.8.2)
