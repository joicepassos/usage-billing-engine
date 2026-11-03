package io.github.joicepassos.billing.accounts.internal;

import io.github.joicepassos.billing.shared.taxid.TaxId;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

/** A tenant's client: who receives invoices, NFS-e and charges. Holds personal data (LGPD). */
@Entity
@Table(name = "customer")
class Customer {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "external_id", nullable = false, updatable = false)
    private String externalId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_id_type", nullable = false)
    private TaxId.Type taxIdType;

    @Column(name = "tax_id", nullable = false)
    private String taxId;

    @Column(nullable = false)
    private String email;

    @Embedded
    private Address address;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Customer() {
    }

    Customer(UUID id, String externalId, String name, TaxId taxId, String email, Address address, Instant now) {
        this.id = id;
        this.externalId = externalId;
        this.createdAt = now;
        update(name, taxId, email, address, now);
    }

    void update(String name, TaxId taxId, String email, Address address, Instant now) {
        this.name = name;
        this.taxIdType = taxId.type();
        this.taxId = taxId.value();
        this.email = email;
        this.address = address;
        this.updatedAt = now;
    }

    UUID id() {
        return id;
    }

    String externalId() {
        return externalId;
    }

    String name() {
        return name;
    }

    TaxId taxId() {
        return new TaxId(taxIdType, taxId);
    }

    String email() {
        return email;
    }

    Address address() {
        return address;
    }

    @Override
    public String toString() {
        return "Customer[id=" + id + ", externalId=" + externalId + "]";
    }

    @Embeddable
    record Address(
            @Column(name = "address_street", nullable = false) String street,
            @Column(name = "address_number", nullable = false) String number,
            @Column(name = "address_complement") String complement,
            @Column(name = "address_district", nullable = false) String district,
            @Column(name = "address_city", nullable = false) String city,
            @Column(name = "address_city_code", nullable = false) String cityCode,
            @Column(name = "address_state", nullable = false) String state,
            @Column(name = "address_postal_code", nullable = false) String postalCode) {

        @Override
        public String toString() {
            return "Address[city=" + city + ", state=" + state + "]";
        }
    }
}
