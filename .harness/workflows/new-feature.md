# New feature

Add behavior without breaking what already works.

1. **Understand the current state** — read the code, contracts, tests, `CONTEXT.md` and `project-context.md` for the affected modules. Run the module's tests and record the starting state (baseline).
2. **Define criteria** — `product-manager` + `idea-to-prd` (short version: problem, acceptance criteria, out of scope). For a small feature, criteria in the request are enough.
3. **Design the minimum** — `architect` + `api-design` if it touches data, contracts, events, module boundaries or auth. Identify what can break (contract consumers, migrations, event listeners).
4. **Plan** — `task-breakdown` if more than 3 files or more than one module. **Plan approval.**
5. **Implement** — `backend-engineer`, one task at a time, keeping compatibility (new optional fields, expand → contract migrations, feature flag if risky).
6. **Test** — `qa-engineer` + `test-generation`: new criteria + module regression + baseline from step 1.
7. **Review** — `code-reviewer` (+ `security-audit` if it touches auth, personal data, money or webhooks).

Final gate: changed files, checks PASSED/FAILED/NOT_RUN compared to the baseline, contract impact, risks and next steps.
