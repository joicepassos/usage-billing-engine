# ADR 0009 — Tier boundary convention
Status: accepted · Date: 2026-11-02

## Context
Graduated tiers like "0–1,000 at R$ 0.010, 1,001–10,000 at R$ 0.008" are ambiguous about whether unit 1,000 belongs to the first or the second tier. Ambiguity here changes amounts.

## Options considered
1. **Inclusive upper bound (`up_to`):** each tier is defined by the last unit it covers.
2. **Exclusive upper bound (`from`/`to`):** each tier starts at its `from` value.

## Decision
Tiers are defined by an inclusive `up_to` value, ordered ascending; the last tier has no `up_to`. A tier covers units `(previous up_to, up_to]`. With tiers `up_to 1,000`, `up_to 10,000` and unbounded, 12,500 units are billed as 1,000 + 9,000 + 2,500, which is R$ 94.50 with the prices in AC-05. Quantities may be fractional (for example minutes with decimals); boundaries apply to the exact quantity. Packages round the quantity up to whole packages.

## Consequences
- Positive: matches how most billing tools describe tiers; AC-05 is unambiguous.
- Negative: none significant.

## How to revert
Change the definition and every stored plan; needs a new ADR and a data migration.
