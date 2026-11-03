package io.github.joicepassos.billing;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.joicepassos.fixtures.TenancyFixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

class TenantOwnershipTest {

    @Test
    void everyEntityIsTenantOwnedOrExplicitlyTenantAgnostic() {
        List<Class<?>> entities = TenantOwnership.entitiesIn("io.github.joicepassos.billing");

        assertThat(entities).as("entities found by the scan").isNotEmpty();
        assertThat(TenantOwnership.violations(entities)).isEmpty();
    }

    @Test
    void theRuleFlagsAnEntityWithoutTenantId() {
        List<Class<?>> fixtures = TenantOwnership.entitiesIn("io.github.joicepassos.fixtures");

        assertThat(TenantOwnership.violations(fixtures)).containsExactly(TenancyFixtures.Unowned.class);
    }
}
