package io.github.joicepassos.billing.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MoneyTest {

    @Test
    void equalityIgnoresScale() {
        assertThat(Money.of("1.0")).isEqualTo(Money.of("1.00"));
        assertThat(Money.of("1.0")).hasSameHashCodeAs(Money.of("1.00"));
    }

    @ParameterizedTest
    @CsvSource({
            "0.125, 0.12",
            "0.135, 0.14",
            "0.005, 0.00",
            "0.015, 0.02",
            "94.4999, 94.50",
            "-0.125, -0.12"
    })
    void invoiceLineRoundingIsHalfEvenToTwoDecimals(String raw, String expected) {
        Money rounded = Money.of(raw).roundedForInvoiceLine();

        assertThat(rounded.amount()).isEqualByComparingTo(expected);
        assertThat(rounded.amount().scale()).isEqualTo(2);
    }

    @Test
    void multiplicationKeepsFullPrecision() {
        Money unitPrice = Money.of("0.0035");

        Money amount = unitPrice.times(new BigDecimal("1234.5"));

        assertThat(amount.amount()).isEqualByComparingTo("4.32075");
    }

    @Test
    void arithmeticAndComparison() {
        Money a = Money.of("10.00");
        Money b = Money.of("2.50");

        assertThat(a.plus(b)).isEqualTo(Money.of("12.5"));
        assertThat(b.minus(a).isNegative()).isTrue();
        assertThat(a.isGreaterThan(b)).isTrue();
        assertThat(b.max(a)).isEqualTo(a);
        assertThat(Money.ZERO.isZero()).isTrue();
    }

    @Test
    void rejectsNullAmount() {
        assertThatNullPointerException().isThrownBy(() -> Money.of((BigDecimal) null));
    }
}
