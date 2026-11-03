package io.github.joicepassos.billing.accounts;

import java.util.UUID;

/** A new tenant and the only copy of its API key. */
public record ProvisionedTenant(UUID tenantId, String name, String apiKey) {

    @Override
    public String toString() {
        return "ProvisionedTenant[tenantId=" + tenantId + ", name=" + name + ", apiKey=****]";
    }
}
