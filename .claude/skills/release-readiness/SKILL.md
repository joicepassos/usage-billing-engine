---
name: release-readiness
description: Evaluates whether a version is ready to release or tag. Build, tests, module verification, configuration, migrations, documentation, observability, demo and recovery plan. Emits a GO / GO with caveats / NO-GO verdict with evidence. Use when asked "can I release?", "is it ready?", "release checklist", "posso publicar?", or before tagging a version or deploying anywhere.
---

# Release Readiness

A quality gate before releasing. This skill **evaluates and recommends**; it does not deploy or push. Releasing always needs explicit human approval.

## Procedure

### 1. Collect automatic evidence
```bash
.harness/scripts/quality-check.sh .          # lists what exists (NOT_RUN)
.harness/scripts/quality-check.sh . --run    # runs ./gradlew check build — only with authorization
```
Without authorization for `--run`, everything stays NOT_RUN and the verdict cannot be a full GO.

### 2. Checklist by area
Mark each item PASSED / FAILED / NOT_RUN / N/A, with evidence (command, file, output).

**Code and build**
- [ ] `./gradlew build` succeeds.
- [ ] `./gradlew check` passes, including `ApplicationModules.verify()` and integration tests.
- [ ] No debug logging left on, no blocking `TODO`, no `@Disabled` without an issue reference.

**Configuration**
- [ ] Every property read in code exists in `application.yml` or is documented in `.env.example`.
- [ ] Default profile uses simulators; real adapters only behind an explicit profile.
- [ ] No secret, certificate or real document number committed.

**Data**
- [ ] Migrations reviewed, forward-only and compatible with the previous release.
- [ ] Seeds and fake data never run outside local and test profiles.

**Security** — run `security-audit` on the diff; no open Critical/High finding.

**Demo and documentation (portfolio)**
- [ ] `docker compose up` brings the stack up from a clean clone.
- [ ] The simulator runs a full month and prints invoice, NFS-e and charge.
- [ ] README explains the problem, architecture diagram, decisions (links to ADRs), how to run and what is out of scope.
- [ ] Generated OpenAPI reviewed; `CONTEXT.md` matches the code.

**Operation**
- [ ] Traces and business metrics visible in the local Grafana.
- [ ] **Recovery plan** written: how to roll back the application version and how to roll forward a migration fix.

### 3. Verdict
| Verdict | When |
|---|---|
| **GO** | Everything critical PASSED; nothing FAILED |
| **GO with caveats** | Non-critical items FAILED/NOT_RUN with the risk explicitly accepted by the user |
| **NO-GO** | Build/tests FAILED, secret exposed, Critical/High security finding, migration without a recovery plan |

## Output template
```markdown
# Release readiness — <version/scope> — YYYY-MM-DD
**Verdict:** NO-GO
**Blockers:** 1) `./gradlew check` FAILED (InvoiceCloseIT) 2) ...
**Acceptable caveats:** ...
**NOT_RUN:** scenario run (Docker not available)
**Recovery:** ...
**Next steps:** ...
```

## Rules
- No deploy, push to the main branch, tag push or migration against a shared database without explicit approval for **that** action.
- NOT_RUN is never treated as PASSED.
