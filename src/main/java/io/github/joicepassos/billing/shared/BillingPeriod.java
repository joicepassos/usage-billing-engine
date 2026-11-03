package io.github.joicepassos.billing.shared;

import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Objects;

/**
 * A calendar month in a billing zone. Starts at local midnight on day 1 (inclusive) and ends at local midnight on
 * day 1 of the next month (exclusive). See ADR 0002.
 */
public record BillingPeriod(YearMonth month, ZoneId zone) {

    public BillingPeriod {
        Objects.requireNonNull(month, "month");
        Objects.requireNonNull(zone, "zone");
    }

    public static BillingPeriod containing(Instant instant, ZoneId zone) {
        return new BillingPeriod(YearMonth.from(instant.atZone(zone)), zone);
    }

    public Instant start() {
        return month.atDay(1).atStartOfDay(zone).toInstant();
    }

    public Instant end() {
        return month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();
    }

    public boolean contains(Instant instant) {
        return !instant.isBefore(start()) && instant.isBefore(end());
    }

    /** The instant after which late events for this period roll into the next one. */
    public Instant graceEnd(Duration graceWindow) {
        return end().plus(graceWindow);
    }

    public BillingPeriod next() {
        return new BillingPeriod(month.plusMonths(1), zone);
    }

    public BillingPeriod previous() {
        return new BillingPeriod(month.minusMonths(1), zone);
    }
}
