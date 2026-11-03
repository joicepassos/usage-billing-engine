package io.github.joicepassos.billing.accounts;

/** Creates tenants. Used by operators and by the simulator. */
public interface TenantProvisioning {

    /**
     * Creates a tenant and its first API key. The plaintext key exists only in the returned value; only its hash
     * is stored.
     */
    ProvisionedTenant provision(String name);
}
