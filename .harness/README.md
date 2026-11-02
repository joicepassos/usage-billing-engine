# Harness PRO — Java/Gradle edition

Role-based agents, skills, rules and workflows for AI-assisted development of a Spring Boot / Gradle backend. Derived from Harness PRO v1.0 (web edition) and adapted for this project.

## Layout

| Path | Contents |
|---|---|
| `CLAUDE.md` | Entry point; points here |
| `AGENTS.md` | Entry point for tools that read `AGENTS.md` |
| `.claude/agents/` | Roles: orchestrator, product-manager, architect, backend-engineer, qa-engineer, code-reviewer |
| `.claude/skills/` | idea-to-prd, task-breakdown, api-design, test-generation, security-audit, release-readiness, debugging, code-review |
| `.claude/rules/` | global, security, java-backend, language |
| `.harness/core/` | orchestrator, project context, execution policy |
| `.harness/workflows/` | new-project, new-feature, fix-bug |
| `.harness/scripts/` | project-analyzer.sh, quality-check.sh |

## Spec artifacts (simple spec)

| File | Produced by | Purpose |
|---|---|---|
| `docs/PRD.md` | idea-to-prd | Problem, scope, acceptance criteria |
| `CONTEXT.md` | architect / product-manager | Domain glossary; names used in code |
| `docs/adr/NNNN-title.md` | architect | Architecture decisions |
| `docs/PLAN.md` | task-breakdown | Ordered tasks linked to acceptance criteria |

## Scripts

```bash
.harness/scripts/project-analyzer.sh .      # read-only summary of the project
.harness/scripts/quality-check.sh .         # lists Gradle checks (NOT_RUN)
.harness/scripts/quality-check.sh . --run   # runs ./gradlew check build
```

Neither script installs anything. `--run` executes the project's own Gradle build; run it only on trusted code.

## What was removed from the web edition

Frontend-only parts: `frontend-engineer`, `ui-designer`, `landing-page`, `react-patterns`, `forms-validation`, `design-system`, the frontend rule and the npm-based plugins. The design step of `new-project` is skipped.
