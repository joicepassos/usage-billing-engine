package io.github.joicepassos.billing.metering.internal;

import java.util.Locale;

/** How a billable metric turns events into usage. */
enum Aggregation {
    SUM, COUNT, MAX, UNIQUE;

    static Aggregation fromApi(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }

    String toApi() {
        return name().toLowerCase(Locale.ROOT);
    }
}
