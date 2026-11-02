---
name: code-review
description: Structured code review of a diff, branch or set of files. Correctness, money and time handling, security, tenant isolation, contracts, module boundaries, transactions, tests, readability and project conventions, with prioritized findings (blocking / important / suggestion) and file:line evidence. Use when asked "review my code", "review the PR", "can I merge?", "revisa isso", or at the end of every implementation task before delivery.
---

# Code Review

Review to **find problems that matter**, not to rewrite in your style.

## 1. Understand before judging
- Get the diff: `git diff`, `git diff main...HEAD`, or the files given.
- Read the goal (task, PRD, acceptance criteria). A review without a goal becomes opinion.
- Read code around the changes (callers, events, listeners, tests). Many bugs live in the interaction, not the changed line.

## 2. What to check (in order of importance)
1. **Correctness:** does it do what the task asks? Edge cases (empty cycle, zero usage, tier edges, null, concurrency, late events, repeated ids)?
2. **Money and time:** `BigDecimal` everywhere, `compareTo`, rounding once with HALF_EVEN, scale explicit; `Clock` injected; UTC storage, `America/Sao_Paulo` periods, inclusive/exclusive bounds consistent.
3. **Security and tenancy:** input validated, tenant from the API key only, `@TenantId` on new entities, no secrets or personal data in logs. For sensitive changes, apply `security-audit`.
4. **Contracts and compatibility:** changed a route, record, event or migration? Are consumers updated? Migration compatible with the previous release? OpenAPI diff reviewed?
5. **Module boundaries and transactions:** no access to another module's `internal`; events instead of direct calls where designed; `@Transactional` at the right level and actually applied; external calls never inside a DB transaction.
6. **Resilience:** external calls through the outbox with timeout/retry/circuit breaker; retries only for idempotent operations; failures mapped to domain states.
7. **Tests:** cover the new behavior and the fixed bug? Test behavior, not implementation? Were they run?
8. **Readability and maintenance:** clear names matching `CONTEXT.md`, single-responsibility methods, records and value objects, no dead code, project conventions followed.
9. **Performance:** N+1 queries, missing indexes, unbounded queries, loops with I/O, large batches in one transaction.
10. **Dependencies:** is a new dependency necessary, maintained, license-compatible with MIT?

## 3. Classify findings
| Level | Meaning | Blocks merge? |
|---|---|---|
| 🔴 **Blocking** | Bug, wrong amount, security flaw, cross-tenant leak, broken contract, data loss, failing test | Yes |
| 🟡 **Important** | Real maintenance risk, likely edge case, missing test for critical logic | Should be fixed |
| 🔵 **Suggestion** | Readability/style improvement; optional | No |

Each finding needs: **where** (file:line), **what**, **why it matters** (concrete scenario) and **suggestion** (ideally a code snippet). If you cannot describe a failure scenario, downgrade or drop it.

## 4. Template
```markdown
# Review — <scope>
**Summary:** <1–2 sentences> · **Verdict:** Approve | Approve with changes | Changes required

## 🔴 Blocking
1. `pricing/internal/GraduatedTierStrategy.java:58` — rounds each tier before summing; a 3-tier charge can be off by up to 2 cents.
   Suggestion: sum unrounded amounts and round once on the invoice line + jqwik property test.

## 🟡 Important
## 🔵 Suggestions
## ✅ Good points
## Checks
./gradlew check PASSED · scenario NOT_RUN (Docker not available)
```

## Rules
- Review what changed; old problems outside the scope go in a "pre-existing" section and do not block.
- Do not raise pure personal taste as blocking.
- Be specific and kind: critique the code, not the person.
- Self-review: when finishing a task, run this skill on your own diff before declaring it done.
