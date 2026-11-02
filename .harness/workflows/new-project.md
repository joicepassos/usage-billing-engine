# New project

From idea to the first releasable version, in approved increments.

| # | Step | Role | Skills | Output | Gate |
|---|---|---|---|---|---|
| 1 | Discovery and PRD | product-manager | idea-to-prd | `docs/PRD.md` | PRD approved |
| 2 | Architecture | architect | api-design, security-audit | modules, data model, contracts, `CONTEXT.md`, ADRs | Design and dependencies approved |
| 3 | Task plan | architect / product-manager | task-breakdown | `docs/PLAN.md` | Plan approved |
| 4 | Incremental build | backend-engineer | api-design, test-generation | code per task | Each task: `./gradlew check` |
| 5 | Review | code-reviewer | code-review, security-audit | report | No blocking findings |
| 6 | Verification | qa-engineer | test-generation | AC × test matrix | Must ACs covered |
| 7 | Readiness | qa-engineer | release-readiness | GO/NO-GO verdict | Human approval to release |

## Rules
- No code before the plan is approved (steps 1–3 produce documents only).
- Adding dependencies, creating accounts or services, and pushing require explicit approval.
- In step 4, parallelize only tasks with no files in common.
- At the end of each step: short summary + what needs a decision.

Final gate: report with files, checks PASSED/FAILED/NOT_RUN, risks and next steps.
