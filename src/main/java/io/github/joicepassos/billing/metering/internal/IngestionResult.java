package io.github.joicepassos.billing.metering.internal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

/** The outcome of a batch, one result per event, in request order. */
record IngestionResult(int accepted, int duplicates, int conflicts, int rejected, List<EventResult> results) {

    static IngestionResult of(List<EventResult> results) {
        return new IngestionResult(count(results, Status.ACCEPTED), count(results, Status.DUPLICATE),
                count(results, Status.CONFLICT), count(results, Status.REJECTED), results);
    }

    private static int count(List<EventResult> results, Status status) {
        return (int) results.stream().filter(result -> result.status() == status).count();
    }

    enum Status {
        ACCEPTED, DUPLICATE, CONFLICT, REJECTED;

        @JsonValue
        String json() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record EventResult(String transactionId, Status status, YearMonth billingPeriod, Instant occurredAt,
            EventError error) {

        static EventResult accepted(String transactionId, YearMonth period, Instant occurredAt) {
            return new EventResult(transactionId, Status.ACCEPTED, period, occurredAt, null);
        }

        static EventResult duplicate(String transactionId, YearMonth period, Instant occurredAt) {
            return new EventResult(transactionId, Status.DUPLICATE, period, occurredAt, null);
        }

        static EventResult conflict(String transactionId) {
            return new EventResult(transactionId, Status.CONFLICT, null, null, new EventError(409,
                    "Transaction id already used with a different payload"));
        }

        static EventResult rejected(String transactionId, String detail) {
            return new EventResult(transactionId, Status.REJECTED, null, null, new EventError(422, detail));
        }
    }

    record EventError(int status, String detail) {
    }
}
