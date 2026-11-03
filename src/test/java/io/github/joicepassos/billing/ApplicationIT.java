package io.github.joicepassos.billing;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.joicepassos.billing.shared.BillingProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ApplicationIT {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    Clock clock;

    @Autowired
    BillingProperties billing;

    @Test
    void migrationsRunAgainstTimescaleDb() {
        Integer timescale = jdbc.queryForObject(
                "select count(*) from pg_extension where extname = 'timescaledb'", Integer.class);
        Integer registry = jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_name = 'event_publication'", Integer.class);

        assertThat(timescale).isEqualTo(1);
        assertThat(registry).isEqualTo(1);
    }

    @Test
    void billingDefaultsAreBound() {
        assertThat(billing.zone()).isEqualTo(ZoneId.of("America/Sao_Paulo"));
        assertThat(billing.graceWindow()).isEqualTo(Duration.ofHours(1));
        assertThat(clock.getZone()).isEqualTo(ZoneId.of("Z"));
    }
}
