package io.github.joicepassos.billing.metering.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** AC-13 and AC-14: the rule as decided at ingestion. Times are UTC; São Paulo is UTC-3. */
class PeriodAssignmentTest {

    private final PeriodAssignment periods =
            new PeriodAssignment(ZoneId.of("America/Sao_Paulo"), Duration.ofHours(1));

    @ParameterizedTest(name = "occurred {0}, received {1} -> {2}")
    @CsvSource({
            // on time
            "2026-11-15T12:00:00Z, 2026-11-15T12:00:05Z, 2026-11",
            // November event, arrives inside the 1h grace window after the period ends (AC-13)
            "2026-12-01T02:59:00Z, 2026-12-01T03:30:00Z, 2026-11",
            // exactly at the grace end still counts for November
            "2026-12-01T02:59:00Z, 2026-12-01T04:00:00Z, 2026-11",
            // after the grace window: next period, original timestamp kept by the caller (AC-14)
            "2026-12-01T02:59:00Z, 2026-12-01T04:00:01Z, 2026-12",
            // a 02:30Z event on December 1st is still November 30th in São Paulo
            "2026-12-01T02:30:00Z, 2026-12-01T02:31:00Z, 2026-11",
            // very late October event while November is still open goes to November, not December
            "2026-10-20T12:00:00Z, 2026-12-01T03:30:00Z, 2026-11",
            // very late event long after: lands in the arrival period
            "2026-06-10T12:00:00Z, 2026-11-20T12:00:00Z, 2026-11",
            // slightly future-dated event (clock skew) keeps its own period
            "2026-12-01T03:02:00Z, 2026-12-01T02:59:00Z, 2026-12"
    })
    void assignsThePeriodTheEventCountsFor(Instant occurredAt, Instant receivedAt, YearMonth expected) {
        assertThat(periods.assign(occurredAt, receivedAt).month()).isEqualTo(expected);
    }
}
