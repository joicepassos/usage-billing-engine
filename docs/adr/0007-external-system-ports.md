# ADR 0007 — Fiscal issuer and payment gateway behind ports
Status: accepted · Date: 2026-11-02

## Context
The national-standard NFS-e API requires an ICP-Brasil digital certificate tied to a real CNPJ, and the payment gateway requires an account. A public portfolio repository must run for anyone, with no certificate, tax id or credentials.

## Decision
- `FiscalIssuer` port in `fiscal`, with a local simulator (WireMock, authorize and reject scenarios) as the default adapter, and a national-standard adapter enabled only by an explicit profile.
- IBS and CBS values come from a rate table with validity dates (2026 test rates: CBS 0.9%, IBS 0.1%), never from constants.
- `PaymentGateway` port in `payments`, with a local stub as default and an Asaas sandbox adapter behind a profile.
- No certificate, real CPF/CNPJ or credential is ever committed. Real (non-sandbox) endpoints are never called by the default setup.

## Consequences
- Positive: the full flow runs offline; providers can be swapped.
- Negative: simulators can drift from the real APIs.
- Risks: contract drift. Mitigation: fixtures recorded from documentation and the sandbox (T-04), contract tests for each adapter.

## How to revert
Remove the simulators once a shared sandbox exists; ports stay.
