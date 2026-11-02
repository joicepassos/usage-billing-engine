# Bug fix

Main skill: `debugging`. Roles: `qa-engineer` (reproduction and regression), `backend-engineer` (fix), `code-reviewer`.

1. **Record** — expected × observed, exact message and stack trace, environment and profile, since when, what changed recently.
2. **Reproduce** — minimal steps; ideally an automated test that **fails**. No reproduction: stop and gather evidence (do not fix blindly).
3. **Isolate and prove the root cause** — explicit hypotheses tested one at a time; explain why the bug happens (file:line).
4. **Fix** — smallest change that removes the cause; no opportunistic refactoring. Look for the same pattern elsewhere and report it.
5. **Verify** — regression test FAILED → PASSED, module suite, `./gradlew check`, original scenario.
6. **Review** — `code-reviewer` on the diff; `security-audit` if the bug has security impact (and assess whether data was exposed).

Final gate: report in the `debugging` template (symptom, root cause, fix, regression, NOT_RUN, risks).
