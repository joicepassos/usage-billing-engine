package io.github.joicepassos.billing.accounts.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface TenantRepository extends JpaRepository<Tenant, UUID> {
}
