package io.github.joicepassos.billing.metering.internal;

import io.github.joicepassos.billing.shared.BillingPeriod;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Decides, once and at ingestion, which billing period an event counts for (ADR 0003, amendment 1). Starting from
 * the period that contains the event, it moves to the next period while the event arrived after that period's
 * grace end. An event therefore never lands in a period that may already have closed.
 */
final class PeriodAssignment {

    private final ZoneId zone;
    private final Duration graceWindow;

    PeriodAssignment(ZoneId zone, Duration graceWindow) {
        this.zone = zone;
        this.graceWindow = graceWindow;
    }

    BillingPeriod assign(Instant occurredAt, Instant receivedAt) {
        BillingPeriod period = BillingPeriod.containing(occurredAt, zone);
        while (receivedAt.isAfter(period.graceEnd(graceWindow))) {
            period = period.next();
        }
        return period;
    }
}
