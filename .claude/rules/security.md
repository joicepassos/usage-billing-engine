# Security
- Validate every input on the server (Bean Validation at the edge, invariants in the domain).
- Check authentication and tenant ownership on every endpoint and query. `tenantId` always comes from the authenticated API key, never from the request body.
- Use parameterized queries or JPA; never concatenate SQL.
- Secrets only in environment variables or a secret manager; never in code, `application.yml` committed values, or commits.
- Minimize personal data (CPF, CNPJ, addresses, emails) in logs, error messages and responses.
- Verify incoming webhooks (provider signature or shared token) and process them idempotently.
- Review new dependencies and never run unknown scripts automatically.
