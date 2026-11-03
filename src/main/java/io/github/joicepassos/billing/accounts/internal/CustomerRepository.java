package io.github.joicepassos.billing.accounts.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Queries are JPQL, so Hibernate applies the {@code @TenantId} restriction to every one of them. Do not load
 * customers with {@code findById}: lookups by primary key are not guaranteed to be tenant-filtered.
 */
interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findOneById(UUID id);

    boolean existsByExternalId(String externalId);
}
