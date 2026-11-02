# Java backend rules (override agent and skill defaults)

## Build
- Gradle with Kotlin DSL; versions only in `gradle/libs.versions.toml`.
- Per-task gate: `./gradlew check`. Never mark a task done with it FAILED or NOT_RUN without saying so.
- Adding or upgrading a dependency needs explicit approval.

## Modules
- Spring Modulith. Each module (`metering`, `pricing`, `invoicing`, `fiscal`, `payments`, `reconciliation`) exposes only its top-level package; everything else lives in `internal`.
- Modules talk through application events or the published API of another module. No reaching into another module's `internal` package or repositories.
- `ApplicationModules.of(...).verify()` must pass.

## Money and time
- Money is `BigDecimal`, never `double`/`float`, never `long` cents. This overrides any "integer cents" default elsewhere in the harness.
- Compare with `compareTo`, not `equals`. Rounding: HALF_EVEN, applied once, on invoice lines.
- Store instants in UTC (`Instant`, `timestamptz`). Billing periods are computed in `America/Sao_Paulo`.
- Inject `java.time.Clock`; never call `Instant.now()` or `LocalDate.now()` directly in domain or application code.

## Persistence
- PostgreSQL + TimescaleDB. Schema changes only through versioned migrations; migrations are forward-only and compatible with the previous release.
- Every tenant-owned entity carries `@TenantId`. An architecture test fails if one does not.
- Transactions live at the application-service level. Watch for `@Transactional` self-invocation.

## Integrations
- External systems (fiscal issuer, payment gateway) sit behind ports (`FiscalIssuer`, `PaymentGateway`) with adapters in `internal`.
- External calls go through the outbox and run asynchronously with Resilience4j timeout, retry with backoff (idempotent operations only) and circuit breaker.
- Default profile uses simulators; real adapters only behind an explicit profile. Never call production endpoints.

## Code style
- Records for DTOs, commands and events. Immutable value objects for money, periods and identifiers.
- No `null` returned from public methods; use `Optional` at query boundaries.
- Errors over HTTP use RFC 9457 Problem Details (`ProblemDetail`) with a `requestId`.
- API documentation is code-first via springdoc annotations.
