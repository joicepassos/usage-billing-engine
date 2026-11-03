-- Personal data (LGPD): tax id, name, email and address are used only to issue NFS-e and charges.
CREATE TABLE customer
(
    id                  UUID PRIMARY KEY,
    tenant_id           UUID                     NOT NULL REFERENCES tenant (id),
    external_id         TEXT                     NOT NULL,
    name                TEXT                     NOT NULL,
    tax_id_type         TEXT                     NOT NULL CHECK (tax_id_type IN ('CPF', 'CNPJ')),
    tax_id              TEXT                     NOT NULL,
    email               TEXT                     NOT NULL,
    address_street      TEXT                     NOT NULL,
    address_number      TEXT                     NOT NULL,
    address_complement  TEXT,
    address_district    TEXT                     NOT NULL,
    address_city        TEXT                     NOT NULL,
    address_city_code   TEXT                     NOT NULL,
    address_state       TEXT                     NOT NULL,
    address_postal_code TEXT                     NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    version             BIGINT                   NOT NULL,
    CONSTRAINT customer_external_id_per_tenant UNIQUE (tenant_id, external_id)
);
