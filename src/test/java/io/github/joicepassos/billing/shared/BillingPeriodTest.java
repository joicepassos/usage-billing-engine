package io.github.joicepassos.billing.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class BillingPeriodTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private final BillingPeriod november = new BillingPeriod(YearMonth.of(2026, 11), SAO_PAULO);

    @Test
    void startsAndEndsAtLocalMidnight() {
        assertThat(november.start()).isEqualTo(Instant.parse("2026-11-01T03:00:00Z"));
        assertThat(november.end()).isEqualTo(Instant.parse("2026-12-01T03:00:00Z"));
    }

    @Test
    void startIsInclusiveAndEndIsExclusive() {
        assertThat(november.contains(Instant.parse("2026-11-01T03:00:00Z"))).isTrue();
        assertThat(november.contains(Instant.parse("2026-12-01T02:59:59.999Z"))).isTrue();
        assertThat(november.contains(Instant.parse("2026-12-01T03:00:00Z"))).isFalse();
        assertThat(november.contains(Instant.parse("2026-11-01T02:59:59Z"))).isFalse();
    }

    @Test
    void anInstantBeforeLocalMidnightBelongsToThePreviousLocalMonth() {
        BillingPeriod period = BillingPeriod.containing(Instant.parse("2026-12-01T02:30:00Z"), SAO_PAULO);

        assertThat(period).isEqualTo(november);
    }

    @Test
    void rollsOverTheYear() {
        BillingPeriod december = new BillingPeriod(YearMonth.of(2026, 12), SAO_PAULO);

        assertThat(december.next().month()).isEqualTo(YearMonth.of(2027, 1));
        assertThat(december.next().previous()).isEqualTo(december);
    }

    @Test
    void graceEndIsTheWindowAfterThePeriodEnds() {
        assertThat(november.graceEnd(Duration.ofHours(1))).isEqualTo(Instant.parse("2026-12-01T04:00:00Z"));
    }
}
