# usage-billing-engine

Usage-based billing engine: ingests usage events, closes monthly cycles, issues invoices, Brazilian service invoices (NFS-e) and payment charges (Pix/boleto), and reconciles payments. Public portfolio project, MIT licensed.

<!-- harness-pro:start -->
## Harness PRO (Java/Gradle edition)

This repository uses Harness PRO. Before any non-trivial task:

1. Read `.harness/core/orchestrator.md` and `.harness/core/project-context.md`.
2. Follow the rules in `.claude/rules/` (`global`, `security`, `java-backend`, `language`). Project rules override any default inside an agent or skill.
3. Pick a workflow: `.harness/workflows/new-project.md`, `new-feature.md` or `fix-bug.md`.
4. Agents live in `.claude/agents/`, skills in `.claude/skills/`. Load only the skills the task needs.
5. Report every check as PASSED / FAILED / NOT_RUN. Never claim something ran when it did not.

Quality gate for every task: `./gradlew check` (see `.harness/scripts/quality-check.sh`).
<!-- harness-pro:end -->
