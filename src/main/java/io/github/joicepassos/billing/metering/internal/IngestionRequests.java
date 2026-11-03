package io.github.joicepassos.billing.metering.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Schema checks here reject the whole batch with 422; business outcomes are reported per event. */
final class IngestionRequests {

    static final int MAX_BATCH = 500;
    private static final String ID = "[A-Za-z0-9._:-]+";
    private static final String ID_MESSAGE = "may contain only letters, digits and . _ : -";

    private IngestionRequests() {
    }

    record EventBatch(@NotNull @Size(min = 1, max = MAX_BATCH) List<@NotNull @Valid UsageEventRequest> events) {
    }

    record UsageEventRequest(
            @NotBlank @Size(max = 100) @Pattern(regexp = ID, message = ID_MESSAGE) String transactionId,
            @NotBlank @Size(max = 100) @Pattern(regexp = ID, message = ID_MESSAGE) String customerExternalId,
            @NotBlank @Size(max = 64) String metricCode,
            @NotNull @PositiveOrZero @Digits(integer = 14, fraction = 6) BigDecimal quantity,
            @NotNull Instant occurredAt,
            @Size(max = 200) String uniqueKey) {
    }
}
