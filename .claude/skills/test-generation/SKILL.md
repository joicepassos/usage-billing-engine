---
name: test-generation
description: Plans and writes automated tests proportional to risk. Unit, property-based (jqwik), integration with Testcontainers and WireMock, module verification and end-to-end scenarios, from acceptance criteria, covering happy path, errors, boundaries and regressions, and reporting PASSED/FAILED/NOT_RUN honestly. Use when asked to "write tests", "cover this", "test plan", "regression test", "escreve os testes", or after implementing a feature or fixing a bug.
---

# Test Generation

Tests that catch real bugs and survive refactoring. Quantity is not the goal; confidence is.

## 1. Discover the setup
- Read `build.gradle.kts`, `gradle/libs.versions.toml`, the test source sets and existing tests. **Follow the existing pattern** (naming `*Test` for unit, `*IT` for integration, shared fixtures, Testcontainers setup).
- Missing a framework? Propose the minimum and **ask for approval** before adding it.

## 2. Decide what to test (by risk)
| Priority | Examples in this project |
|---|---|
| High | Pricing math and rounding, cycle boundaries and time zones, idempotent ingestion, tenant isolation, invoice state machine, NFS-e rejection path, webhook handling, reconciliation |
| Medium | Late events and the grace window, unknown customers, outbox retries, overdue job |
| Low | Trivial mappers, getters, framework behavior |

Map every acceptance criterion (AC) to at least one test.

## 3. Pick the level
- **Unit (JUnit 5 + AssertJ):** domain logic, value objects, pricing strategies. Fast, many cases.
- **Property-based (jqwik):** invariants of money and tiers (sum of lines equals total, rounding never loses more than one cent per line, graduated tiers are monotonic).
- **Module (Spring Modulith):** `ApplicationModules.of(App.class).verify()` and `@ApplicationModuleTest` for a module in isolation with published events asserted.
- **Integration (Testcontainers + WireMock):** real Postgres/TimescaleDB, simulated fiscal issuer and payment gateway. Assert HTTP contract, persistence and outbox behavior.
- **Scenario:** the simulator runs a full month for one tenant and the test asserts invoice, NFS-e and charge. Few and stable.

## 4. Cases per feature
For each unit, consider:
- happy path;
- invalid input (each validation rule);
- boundaries (zero, one, tier edges, max batch size, last second of the month, DST-free but offset-sensitive dates, large quantities, scale of unit prices);
- tenant isolation (other tenant's id → 404);
- external failure (timeout, 5xx, malformed response, business rejection);
- idempotency and duplicates (same transaction_id, repeated webhook);
- concurrency where relevant (two cycle closes, simultaneous ingestion of the same id).

## 5. Writing good tests
- The name describes behavior: `repeatedTransactionIdReturnsOriginalResponse`.
- Arrange / Act / Assert; one behavior per test.
- Mock **only at boundaries** (network via WireMock, clock via a fixed `Clock`). Do not mock the class under test or the database.
- Determinism: fixed `Clock`, fixed seeds, no `Thread.sleep`; use Awaitility for async outcomes.
- Fake test data only; **never** real CPF/CNPJ, certificates or production secrets. Use generated valid-format documents.
- `BigDecimal` assertions with `isEqualByComparingTo`.

### Regression test for a bug
1. Write the test that reproduces the bug **before** the fix and confirm it fails.
2. Fix.
3. Confirm it passes. Record before/after in the report.

## 6. Run and report
Run only project commands (`./gradlew check`, `./gradlew test --tests '...'`). Report:
```
| Suite | Command | Result | Note |
| unit | ./gradlew test --tests '*Pricing*' | PASSED (24/24) | |
| integration | ./gradlew check | NOT_RUN | Docker not available |
```
**Never** write PASSED without running. A test failing for an unrelated reason: report it; do not delete or `@Disabled` it without saying so.

## Checklist
- [ ] Every AC has at least one mapped test.
- [ ] Fixed bugs have a regression test that failed before.
- [ ] No assertions on implementation details.
- [ ] No flaky tests (time, order, real network).
- [ ] Actual run results reported.
