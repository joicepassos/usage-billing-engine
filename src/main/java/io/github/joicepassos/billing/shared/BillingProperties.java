package io.github.joicepassos.billing.shared;

import java.time.Duration;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Engine-wide billing settings.
 *
 * @param zone zone in which billing periods are computed
 * @param graceWindow how long after a period ends late events still count for it
 */
@ConfigurationProperties("billing")
public record BillingProperties(
        @DefaultValue("America/Sao_Paulo") ZoneId zone,
        @DefaultValue("1h") Duration graceWindow) {
}
