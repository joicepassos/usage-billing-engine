# ADR 0002 — Money and time representation
Status: accepted · Date: 2026-11-02

## Context
Invoices must be exact to the cent. Unit prices can be fractions of a cent (R$ 0.0035 per minute). Billing periods are Brazilian calendar months, while events arrive from systems in any time zone.

## Options considered
1. **`long` cents.** Exact for totals; cannot represent sub-cent unit prices without a second scale.
2. **`double`.** Binary floating point; unacceptable for money.
3. **`BigDecimal` with explicit scales and one rounding point.**

## Decision
- Money is `BigDecimal` in BRL, wrapped in a `Money` value object. Unit prices keep up to 6 decimal places; amounts on invoice lines are rounded once, with `HALF_EVEN`, to 2 decimal places. Totals are sums of already-rounded lines. Comparisons use `compareTo`.
- Instants are stored in UTC (`Instant`, `timestamptz`). A `BillingPeriod` is a calendar month in `America/Sao_Paulo`: start inclusive at local midnight on day 1, end exclusive at local midnight on day 1 of the next month.
- Code never reads the system clock directly; a `java.time.Clock` bean is injected.

## Consequences
- Positive: exact arithmetic, testable time, unambiguous period boundaries.
- Negative: more verbose arithmetic than primitives.
- Risks: rounding in more than one place. Mitigation: property-based tests on pricing (AC-08).

## How to revert
Not expected. Changing rounding rules requires a new ADR and migration of stored amounts.
