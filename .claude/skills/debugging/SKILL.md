---
name: debugging
description: Systematic debugging. Reproduce, isolate, form hypotheses, prove the root cause, fix with the smallest change and protect it with a regression test. Use when there is an error, stack trace, failing test, wrong amount on an invoice, "it broke", "works locally but not in CI", "deu erro", unexpected behavior, or when following the fix-bug workflow.
---

# Debugging

Fix the **cause**, not the symptom. No fix without a demonstrated root cause.

## Golden rule
Do not change code "to see if it helps". Each change tests an explicit hypothesis; if it does not confirm, revert it before the next one.

## Procedure

### 1. Record the symptom
- Expected × observed behavior, with the **exact** error message and stack trace.
- Where: local, CI, which profile, which tenant, which billing period, time of day.
- Since when: recent commit, dependency upgrade, migration, configuration change? (`git log --since`, diff of `libs.versions.toml`).

### 2. Reproduce
- Find the minimal steps that always reproduce it. If intermittent, note frequency and conditions (concurrency, ordering of events, clock, specific data).
- Ideal: turn the reproduction into a **failing automated test** (see `test-generation`).
- Cannot reproduce? Do not fix blindly. Gather more evidence (logs, traces, temporary instrumentation) and report.

### 3. Isolate
- Read the stack trace from the bottom up to the first **project** frame; for Spring, look past proxies to the actual bean.
- Bisect: `git bisect` between the last good and the first bad commit, or short-circuit half the flow.
- Check boundaries: is the data entering each layer what you expect? Inspect at each step (HTTP → controller → application service → domain → repository → event → listener → outbox → adapter).
- Compare environments: profiles, properties, Testcontainers image versions, JVM version, time zone.

### 4. Hypotheses
List 2–4 hypotheses ordered by likelihood × cost to check. For each: "If it is X, then doing Y should show Z." Test the cheapest first.

Frequent suspects in this stack:
- `@Transactional` not applied (self-invocation, private method, wrong proxy) or rolled back silently.
- Event listener runs before commit, or an event publication stays incomplete in the Modulith registry.
- Lazy loading outside a transaction; Hibernate flush order; `@TenantId` filter not active in a job or async thread.
- `BigDecimal` compared with `equals` (scale), wrong scale, rounding applied twice.
- Time: `Instant.now()` instead of the injected `Clock`, period computed in UTC instead of `America/Sao_Paulo`, inclusive vs exclusive period end.
- Continuous aggregate not refreshed for the period being closed.
- Outbox retry hitting a non-idempotent operation; webhook processed twice.
- Testcontainers startup or port issues; tests sharing state through the database.
- Dependency upgrade with a behavioral change.

### 5. Prove the root cause
You only have the cause when you can explain **why** the symptom happens and predict when it **does not**. Use "5 whys" so you do not stop at the symptom.

### 6. Fix
- Smallest change that removes the cause. No opportunistic refactoring in the same patch.
- Look for the same pattern elsewhere (`grep`); report it, and fix only if in scope.
- Remove temporary logs and instrumentation.

### 7. Verify
- Regression test: failed before, passes after.
- Run the related suite and `./gradlew check`. Report PASSED/FAILED/NOT_RUN.
- Re-run the original scenario if possible (simulator).

## Report template
```markdown
## Bug: <title>
- **Symptom:** ...
- **Reproduction:** steps / test `...IT.method`
- **Root cause:** <explanation + file:line>
- **Fix:** <what changed and why it is the smallest change>
- **Regression:** test X — FAILED before, PASSED after
- **Same pattern elsewhere:** ...
- **Risks / NOT_RUN:** ...
```

## Traps
- Wrapping in `try/catch` and swallowing the exception.
- Adding null checks everywhere without understanding why the value is null.
- Raising timeouts to "fix" a race condition.
- Declaring it fixed without reproducing the original scenario.
