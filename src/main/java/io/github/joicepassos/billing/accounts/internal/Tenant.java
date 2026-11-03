package io.github.joicepassos.billing.accounts.internal;

import io.github.joicepassos.billing.shared.tenancy.TenantAgnostic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant")
@TenantAgnostic(reason = "A tenant is the owner itself; it is looked up before any tenant is known.")
class Tenant {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Tenant() {
    }

    Tenant(UUID id, String name, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }

    UUID id() {
        return id;
    }

    String name() {
        return name;
    }
}
