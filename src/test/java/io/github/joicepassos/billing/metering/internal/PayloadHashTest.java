package io.github.joicepassos.billing.metering.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PayloadHashTest {

    private static final Instant AT = Instant.parse("2026-11-15T12:00:00Z");

    @Test
    void sameContentGivesTheSameHashEvenWithADifferentScale() {
        assertThat(PayloadHash.of("cus_1", "minutes", new BigDecimal("1.50"), AT, null))
                .isEqualTo(PayloadHash.of("cus_1", "minutes", new BigDecimal("1.5"), AT, null));
    }

    @Test
    void anyFieldChangesTheHash() {
        String base = PayloadHash.of("cus_1", "minutes", BigDecimal.ONE, AT, null);

        assertThat(PayloadHash.of("cus_2", "minutes", BigDecimal.ONE, AT, null)).isNotEqualTo(base);
        assertThat(PayloadHash.of("cus_1", "calls", BigDecimal.ONE, AT, null)).isNotEqualTo(base);
        assertThat(PayloadHash.of("cus_1", "minutes", BigDecimal.TEN, AT, null)).isNotEqualTo(base);
        assertThat(PayloadHash.of("cus_1", "minutes", BigDecimal.ONE, AT.plusMillis(1), null)).isNotEqualTo(base);
        assertThat(PayloadHash.of("cus_1", "minutes", BigDecimal.ONE, AT, "k")).isNotEqualTo(base);
    }

    @Test
    void fieldsCannotBleedIntoEachOther() {
        assertThat(PayloadHash.of("ab", "c", BigDecimal.ONE, AT, null))
                .isNotEqualTo(PayloadHash.of("a", "bc", BigDecimal.ONE, AT, null));
    }
}
