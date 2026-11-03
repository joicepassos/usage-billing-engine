package io.github.joicepassos.billing.shared.tenancy;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exempts an entity from the rule that every entity carries {@code @TenantId}. Only for data that must be read
 * before the tenant is known (tenants themselves, API keys). The reason is mandatory and reviewed.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TenantAgnostic {

    String reason();
}
