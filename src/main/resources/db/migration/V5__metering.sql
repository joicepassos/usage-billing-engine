-- Metering tables (ADR 0003 and 0004, both with amendment 1). No foreign keys across modules (ADR 0001).

CREATE TABLE billable_metric
(
    id          UUID PRIMARY KEY,
    tenant_id   UUID                     NOT NULL,
    code        TEXT                     NOT NULL,
    name        TEXT                     NOT NULL,
    aggregation TEXT                     NOT NULL CHECK (aggregation IN ('SUM', 'COUNT', 'MAX', 'UNIQUE')),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT billable_metric_code_per_tenant UNIQUE (tenant_id, code)
);

-- Idempotency ledger. Lives outside the hypertable because TimescaleDB unique indexes must include the
-- partitioning column.
CREATE TABLE ingested_transaction
(
    tenant_id      UUID                     NOT NULL,
    transaction_id TEXT                     NOT NULL,
    payload_hash   BYTEA                    NOT NULL,
    occurred_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    billing_period DATE                     NOT NULL CHECK (EXTRACT(DAY FROM billing_period) = 1),
    accepted_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (tenant_id, transaction_id)
);

CREATE TABLE usage_event
(
    occurred_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    tenant_id            UUID                     NOT NULL,
    transaction_id       TEXT                     NOT NULL,
    customer_external_id TEXT                     NOT NULL,
    metric_code          TEXT                     NOT NULL,
    quantity             NUMERIC(20, 6)           NOT NULL CHECK (quantity >= 0),
    unique_key           TEXT,
    billing_period       DATE                     NOT NULL CHECK (EXTRACT(DAY FROM billing_period) = 1),
    received_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    payload_hash         BYTEA                    NOT NULL
);

SELECT create_hypertable('usage_event', by_range('occurred_at', INTERVAL '7 days'));

-- Serves the cycle close, which filters by billing period (ADR 0004, amendment 1).
CREATE INDEX usage_event_close_idx ON usage_event (tenant_id, billing_period, customer_external_id, metric_code);
