package io.github.joicepassos.billing.accounts.internal;

import io.github.joicepassos.billing.shared.tenancy.TenantAgnostic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_key")
@TenantAgnostic(reason = "Keys are resolved to find the tenant, so they cannot be filtered by tenant.")
class ApiKey {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "lookup_prefix", nullable = false, unique = true)
    private String lookupPrefix;

    @Column(name = "secret_hash", nullable = false)
    private byte[] secretHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected ApiKey() {
    }

    ApiKey(UUID id, UUID tenantId, String lookupPrefix, byte[] secretHash, Instant createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.lookupPrefix = lookupPrefix;
        this.secretHash = secretHash.clone();
        this.createdAt = createdAt;
    }

    UUID id() {
        return id;
    }

    UUID tenantId() {
        return tenantId;
    }

    byte[] secretHash() {
        return secretHash.clone();
    }

    boolean isRevoked() {
        return revokedAt != null;
    }
}
