---
name: idea-to-prd
description: Turns a loose idea or feature into a PRD with problem, actors, journeys, MVP scope, out of scope, testable acceptance criteria and risks. Use when someone says "I have an idea", "write the requirements", "build the PRD", "what goes into the MVP", "tenho uma ideia", "monta o PRD", or before any new project or large feature without a written spec.
---

# Idea to PRD

Turns an idea into a short, testable, approved PRD before any code. The PRD is the source of truth for `task-breakdown` and the implementation roles.

## When to use
- New project without `docs/PRD.md`.
- Large feature whose scope is still vague.
- A request mixing several ideas that needs an MVP cut.

**Do not use** for bugs, small tweaks, or when an approved PRD already exists. Read it and go to `task-breakdown`.

## Procedure

### 1. Discovery (max. 5 questions per round)
Before asking, read `.harness/core/project-context.md`, `CONTEXT.md`, README and existing code. The approved decisions there are settled. Ask only what blocks:
1. Who is the actor (API client, operator, end customer) and what concrete pain do they have today?
2. What measurable outcome defines success?
3. What is the single most important thing the actor must be able to do?
4. Constraints: deadline, mandatory stack, integrations, LGPD/personal data, fiscal rules.
5. What must definitely **not** go in now?

If the answer is unknown, propose an explicit assumption marked `[ASSUMPTION]`.

### 2. MVP cut
List every feature mentioned and classify with MoSCoW:
- **Must**: without it the main goal does not happen.
- **Should**: important, but there is a manual workaround.
- **Could**: nice to have.
- **Won't (now)**: goes to "Out of scope".

Rule of thumb: the MVP fits in 1–3 main journeys. If it does not, cut.

### 3. Acceptance criteria
Write each criterion in a verifiable format (Given/When/Then). Forbidden: "must be fast", "robust", "easy to integrate". Replace with something measurable: "a batch of 500 events is persisted and acknowledged with 202 in under 500 ms p95 on the local stack", "a repeated transaction_id returns the original response".

### 4. Draft → approval → file
Show the PRD in chat. **Only save to `docs/PRD.md` after explicit approval.** If the file exists, propose a diff; never overwrite.

## Output template

```markdown
# PRD — <product or feature>
Version: 0.1 · Status: draft | approved · Date: YYYY-MM-DD

## 1. Problem
<pain in 2–3 sentences, from the actor's point of view>

## 2. Actors
| Actor | Context | Main pain | Expected gain |

## 3. Goals and success metrics
| Goal | Metric | Target | How measured |

## 4. Main journeys
J1 — <name>: step 1 → step 2 → outcome

## 5. MVP scope
| ID | Feature | Priority (Must/Should/Could) | Journey |

## 6. Out of scope (now)
- ...

## 7. Acceptance criteria
- AC-01 (F-01): Given ..., when ..., then ...

## 8. Non-functional requirements
Performance (ingestion throughput, cycle-close time), reliability (retries, idempotency, no lost events), security, privacy/LGPD, observability, local reproducibility.

## 9. Assumptions
- [ASSUMPTION] ...

## 10. Risks and open questions
| Risk/question | Impact | Mitigation/owner |
```

## Quality checklist
- [ ] Every Must item has at least one acceptance criterion.
- [ ] No criterion uses a vague adjective without a number or observable behavior.
- [ ] "Out of scope" has at least 3 items (sign of a real cut).
- [ ] Personal data (CPF, CNPJ, addresses) identified, with purpose and retention noted.
- [ ] No invented numbers or market data; use `[ASSUMPTION]` or `[TO CONFIRM]`.

## Common traps
- Describing a solution ("use Kafka") instead of a need. Technical choices belong to `architect`.
- An MVP with 15 Must items. If everything is Must, nothing is.
- Ignoring the first run, the empty cycle and the failure path.

## Next step
After approval: `task-breakdown`.
