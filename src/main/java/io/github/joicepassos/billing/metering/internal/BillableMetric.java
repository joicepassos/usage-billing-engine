package io.github.joicepassos.billing.metering.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

/** Something a tenant measures, and how its events aggregate. */
@Entity
@Table(name = "billable_metric")
class BillableMetric {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, updatable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Aggregation aggregation;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected BillableMetric() {
    }

    BillableMetric(UUID id, String code, String name, Aggregation aggregation, Instant createdAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.aggregation = aggregation;
        this.createdAt = createdAt;
    }

    UUID id() {
        return id;
    }

    String code() {
        return code;
    }

    String name() {
        return name;
    }

    Aggregation aggregation() {
        return aggregation;
    }
}
