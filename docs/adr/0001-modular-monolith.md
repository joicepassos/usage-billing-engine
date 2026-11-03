# ADR 0001 — Modular monolith with Spring Modulith
Status: accepted · Date: 2026-11-02

## Context
The engine has clear sub-domains (accounts, metering, pricing, invoicing, fiscal, payments, reconciliation) that must stay decoupled, but it is built and run by one person, and the demo must start with a single `docker compose up`.

## Options considered
1. **Microservices**, one per sub-domain. Strong isolation; high operational cost (deployments, network failures, distributed transactions) for one developer.
2. **Layered monolith** (controllers/services/repositories). Simple; boundaries erode quickly.
3. **Modular monolith with Spring Modulith.** One deployable, boundaries verified by tests, modules communicate through events.

## Decision
Option 3. Modules: `accounts`, `metering`, `pricing`, `invoicing`, `fiscal`, `payments`, `reconciliation`. Each exposes only its top-level package; implementation lives in `internal`. A small open module `shared` holds money, periods, error handling and the outbox, with no business rules. Modules communicate through application events with the Modulith Event Publication Registry, or through another module's published API. `ApplicationModules.verify()` runs in CI.

## Consequences
- Positive: one process, one database, transactional consistency inside a module, boundaries enforced automatically, any module can be extracted later because it already talks through events.
- Negative: all modules share one deployment and one database instance; a bad migration affects everything.
- Risks: `shared` growing into a dumping ground. Mitigation: reviewers reject business rules in `shared`.

## How to revert
Extract a module into its own service by replacing in-process events with a broker and its published API with HTTP. Each extraction gets its own ADR.
