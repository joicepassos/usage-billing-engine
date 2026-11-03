package io.github.joicepassos.billing.shared.tenancy;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * The tenant the current code runs for. Set by the API key filter for requests, and explicitly by jobs. Code that
 * runs with no tenant sees no tenant-owned data. See ADR 0008.
 */
public final class TenantContext {

    /** Marks system work (jobs) that must see every tenant. Never assigned to a real tenant. */
    public static final UUID SYSTEM = new UUID(0L, 1L);

    /** Used when no tenant is set: matches no rows. Never assigned to a real tenant. */
    public static final UUID NONE = new UUID(0L, 0L);

    private static final ScopedValue<UUID> CURRENT = ScopedValue.newInstance();

    private TenantContext() {
    }

    public static Optional<UUID> current() {
        return CURRENT.isBound() ? Optional.of(CURRENT.get()) : Optional.empty();
    }

    public static <T> T callAs(UUID tenantId, Callable<T> work) throws Exception {
        return ScopedValue.where(CURRENT, tenantId).call(work::call);
    }

    public static void runAs(UUID tenantId, Runnable work) {
        ScopedValue.where(CURRENT, tenantId).run(work);
    }
}
