package io.github.joicepassos.billing.accounts.internal;

import io.github.joicepassos.billing.accounts.ProvisionedTenant;
import io.github.joicepassos.billing.accounts.TenantProvisioning;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AccountsService implements TenantProvisioning {

    private final TenantRepository tenants;
    private final ApiKeyRepository keys;
    private final Clock clock;
    private final ApiKeySecrets secrets = new ApiKeySecrets(new SecureRandom());

    AccountsService(TenantRepository tenants, ApiKeyRepository keys, Clock clock) {
        this.tenants = tenants;
        this.keys = keys;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ProvisionedTenant provision(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tenant name is required");
        }
        Tenant tenant = tenants.save(new Tenant(UUID.randomUUID(), name.strip(), clock.instant()));
        String key = secrets.generate();
        keys.save(new ApiKey(UUID.randomUUID(), tenant.id(), ApiKeySecrets.lookupPrefix(key).orElseThrow(),
                ApiKeySecrets.hash(key), clock.instant()));
        return new ProvisionedTenant(tenant.id(), tenant.name(), key);
    }

    /** Resolves a presented key to its tenant, or empty when the key is malformed, unknown or revoked. */
    @Transactional(readOnly = true)
    Optional<AuthenticatedTenant> authenticate(String presentedKey) {
        return ApiKeySecrets.lookupPrefix(presentedKey)
                .flatMap(keys::findByLookupPrefix)
                .filter(key -> !key.isRevoked())
                .filter(key -> ApiKeySecrets.matches(presentedKey, key.secretHash()))
                .map(key -> new AuthenticatedTenant(key.tenantId(), key.id()));
    }

    @Transactional(readOnly = true)
    Optional<Tenant> find(UUID tenantId) {
        return tenants.findById(tenantId);
    }
}
