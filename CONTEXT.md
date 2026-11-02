# Domain glossary

Names in code follow this file. Update it whenever a new domain term appears.

| Term | Meaning |
|---|---|
| **Tenant** | A company that uses the engine to bill its own customers (e.g. a transcription API startup). Authenticates with an API key. |
| **Customer** | A tenant's client who receives invoices, service invoices and charges. Identified by an external id chosen by the tenant. |
| **Billable metric** | Something the tenant measures, with an aggregation: `sum`, `count`, `max` or `unique` (e.g. `transcribed_minutes`, sum). |
| **Usage event** | One measurement sent by the tenant: transaction id, customer external id, metric code, quantity, timestamp. |
| **Transaction id** | Tenant-chosen id that makes an event idempotent within the tenant. |
| **Grace window** | Short period after a billing period ends during which late events still count for that period. |
| **Plan** | A set of charges a customer subscribes to. |
| **Charge** | A billable metric plus a pricing model inside a plan. |
| **Pricing model** | How usage becomes an amount: per unit, graduated tiers, package, monthly minimum. |
| **Subscription** | A customer on a plan, billed per calendar month. Plan changes apply from the next period. |
| **Billing period** | A calendar month in `America/Sao_Paulo`. |
| **Cycle close** | The job that ends a billing period and drafts invoices from aggregated usage. |
| **Invoice** | Amount owed by a customer for a billing period. States: `draft`, `on_hold`, `finalized`, `fiscal_rejected`, `issued`, `paid`, `overdue`, `cancelled`. |
| **Invoice line** | One charge's amount on an invoice; the only place rounding happens. |
| **Fiscal document** | The service invoice issued for a finalized invoice. States: `pending`, `authorized`, `rejected`, `cancelled`. |
| **NFS-e** | *Nota Fiscal de Serviço eletrônica*, the Brazilian electronic service invoice, issued here through the national standard. |
| **DPS** | *Declaração de Prestação de Serviço*, the document sent to the national NFS-e issuer to request an NFS-e. |
| **ISS** | Municipal service tax reported on the NFS-e. |
| **CBS / IBS** | New federal (CBS) and state/municipal (IBS) consumption taxes from the tax reform; 2026 is a test year with rates shown on documents. |
| **CNPJ / CPF** | Brazilian tax ids for companies and individuals. Personal data; never logged. |
| **Payment charge** | A request for payment created in the payment gateway (Pix or boleto) for an issued invoice. |
| **Pix / boleto** | Brazilian instant payment and bank slip payment methods. |
| **Reconciliation** | Matching a payment confirmed by the gateway with its invoice and settling it. |
| **Fiscal issuer** | Port to the NFS-e issuer; local simulator by default, national-standard adapter behind a profile. |
| **Payment gateway** | Port to the payment provider; Asaas sandbox adapter. |
| **Outbox** | Table of pending external calls processed asynchronously with retries. |
