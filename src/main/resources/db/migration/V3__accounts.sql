CREATE TABLE tenant
(
    id         UUID PRIMARY KEY,
    name       TEXT                     NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE api_key
(
    id            UUID PRIMARY KEY,
    tenant_id     UUID                     NOT NULL REFERENCES tenant (id),
    lookup_prefix TEXT                     NOT NULL UNIQUE,
    secret_hash   BYTEA                    NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at    TIMESTAMP WITH TIME ZONE
);

CREATE INDEX api_key_tenant_idx ON api_key (tenant_id);
