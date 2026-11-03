package io.github.joicepassos.billing.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * An amount in BRL. Arithmetic keeps full precision; {@link #roundedForInvoiceLine()} is the only rounding point
 * (two decimals, HALF_EVEN), applied once per invoice line. Equality ignores scale: R$ 1.0 equals R$ 1.00.
 */
public final class Money implements Comparable<Money> {

    public static final int INVOICE_SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        this.amount = Objects.requireNonNull(amount, "amount");
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    public static Money of(String amount) {
        return new Money(new BigDecimal(amount));
    }

    public BigDecimal amount() {
        return amount;
    }

    public Money plus(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money minus(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    /** Multiplies by a quantity without rounding. */
    public Money times(BigDecimal quantity) {
        return new Money(amount.multiply(Objects.requireNonNull(quantity, "quantity")));
    }

    public Money roundedForInvoiceLine() {
        return new Money(amount.setScale(INVOICE_SCALE, ROUNDING));
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isGreaterThan(Money other) {
        return compareTo(other) > 0;
    }

    public Money max(Money other) {
        return isGreaterThan(other) ? this : other;
    }

    @Override
    public int compareTo(Money other) {
        return amount.compareTo(other.amount);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Money other && amount.compareTo(other.amount) == 0;
    }

    @Override
    public int hashCode() {
        return amount.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return "BRL " + amount.toPlainString();
    }
}
