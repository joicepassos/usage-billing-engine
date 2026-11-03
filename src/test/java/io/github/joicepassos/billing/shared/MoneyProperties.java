package io.github.joicepassos.billing.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.BigRange;
import net.jqwik.api.constraints.Scale;
import net.jqwik.api.constraints.Size;

class MoneyProperties {

    private static final BigDecimal HALF_CENT = new BigDecimal("0.005");

    @Property
    void roundingMovesAnAmountByAtMostHalfACent(
            @ForAll @BigRange(min = "-1000000", max = "1000000") @Scale(6) BigDecimal raw) {
        Money rounded = Money.of(raw).roundedForInvoiceLine();

        assertThat(rounded.amount().subtract(raw).abs()).isLessThanOrEqualTo(HALF_CENT);
        assertThat(rounded.amount().scale()).isEqualTo(Money.INVOICE_SCALE);
    }

    @Property
    void roundingIsIdempotent(@ForAll @BigRange(min = "-1000000", max = "1000000") @Scale(6) BigDecimal raw) {
        Money once = Money.of(raw).roundedForInvoiceLine();

        assertThat(once.roundedForInvoiceLine()).isEqualTo(once);
    }

    @Property
    void sumOfRoundedLinesStaysWithinHalfACentPerLineOfTheExactSum(
            @ForAll @Size(min = 1, max = 20) List<@BigRange(min = "0", max = "100000") @Scale(6) BigDecimal> lines) {
        Money exact = lines.stream().map(Money::of).reduce(Money.ZERO, Money::plus);
        Money rounded = lines.stream().map(Money::of).map(Money::roundedForInvoiceLine).reduce(Money.ZERO, Money::plus);

        BigDecimal tolerance = HALF_CENT.multiply(BigDecimal.valueOf(lines.size()));
        assertThat(rounded.minus(exact).amount().abs()).isLessThanOrEqualTo(tolerance);
    }
}
