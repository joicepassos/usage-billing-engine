package io.github.joicepassos.fixtures;

import io.github.joicepassos.billing.shared.tenancy.TenantAgnostic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

/** Entities outside the application package, used only to prove the tenant ownership rule fails when it should. */
public final class TenancyFixtures {

    private TenancyFixtures() {
    }

    @Entity
    public static class Unowned {
        @Id
        UUID id;
    }

    @Entity
    public static class Owned {
        @Id
        UUID id;

        @TenantId
        UUID tenantId;
    }

    @Entity
    @TenantAgnostic(reason = "fixture")
    public static class Agnostic {
        @Id
        UUID id;
    }
}
