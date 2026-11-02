---
name: task-breakdown
description: Splits a PRD, epic or feature into small, ordered, verifiable tasks with dependencies, likely files, acceptance criteria and risks. Use when asked to "break into tasks", "build the backlog", "implementation plan", "where do I start", "quebra em tarefas", "monta o plano", or right after a PRD is approved.
---

# Task Breakdown

Turns approved scope into an execution plan that an agent or a person can implement in small increments, each verifiable and reversible.

## When to use
- There is an approved PRD or a clear feature scope.
- The change touches more than 3 files or more than one module.

**Do not use** for trivial changes (1 file, < 30 lines). Implement directly.

## Procedure

1. **Read the source.** PRD, acceptance criteria, `CONTEXT.md` and current code. Run `.harness/scripts/project-analyzer.sh .` if the structure is unclear. Do not assume package layout.
2. **Slice vertically.** Prefer slices that deliver value end to end (endpoint + domain + persistence + test for one journey) over horizontal layers ("all entities first").
3. **Size.** Each task must:
   - fit in ~1 focused session (ideally < 300 changed lines);
   - have an observable result (endpoint responding, test passing, simulator output);
   - be revertible on its own.
   If it does not fit, split again.
4. **Order by risk.** Put the most uncertain things early (fiscal issuer contract, gateway webhooks, continuous aggregates, cycle-close job). Shared foundations (value objects, tenant context, error format) before what depends on them.
5. **Map dependencies and conflicts.** Two tasks editing the same file are **not** parallelizable. Say so explicitly.
6. **Link to criteria.** Every task references at least one AC; every AC appears in some task.
7. **Define verification.** For each task: how to prove it is done (test class, `./gradlew` command, simulator run).

## Output template

```markdown
# Implementation plan — <feature>
Source: docs/PRD.md v0.1

## Overview
<2–3 sentences + optional diagram of the order>

## Tasks
### T-01 — <verb + outcome> · Size: S/M/L · Role: backend-engineer
- **Goal:** ...
- **Covers:** AC-01, AC-03
- **Module:** metering
- **Likely files:** `src/main/java/.../metering/...` (create), `src/main/resources/db/migration/...` (create)
- **Depends on:** —
- **Parallel with:** T-03 (no shared files)
- **Done when:** <test/command/observable check>
- **Risks:** ...

## Coverage matrix
| AC | Tasks |

## Cross-cutting risks and pending decisions
```

## Quality checklist
- [ ] No "L" task without justification; if there is one, try to split it.
- [ ] Every task has an observable "Done when", not "implement X".
- [ ] Coverage matrix with no orphan AC.
- [ ] Parallelism only between tasks with no shared files.
- [ ] Migrations, auth, money and external integrations flagged high risk with a recovery plan.

## Common traps
- A "set up the project" task that takes two days. Be specific: which plugins, which tasks, which commands.
- Forgetting invisible tasks: error paths, retries, observability, migrations, `.env.example`, README, ADRs.
- Ordering by what is fun instead of what unblocks others.

## Next step
Show the plan and ask for approval. Save to `docs/PLAN.md` once approved. Then execute one task at a time, reporting at the end of each: changed files, checks (PASSED/FAILED/NOT_RUN) and deviations from the plan.
