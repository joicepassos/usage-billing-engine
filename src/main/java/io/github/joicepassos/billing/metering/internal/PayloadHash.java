package io.github.joicepassos.billing.metering.internal;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Fingerprint of an event's content, used to tell a retry (same payload) from a conflicting reuse of a
 * transaction id. Fields are length-prefixed, so no value can imitate another field; quantities compare by value.
 */
final class PayloadHash {

    private PayloadHash() {
    }

    static String of(String customerExternalId, String metricCode, BigDecimal quantity, Instant occurredAt,
            String uniqueKey) {
        StringBuilder canonical = new StringBuilder();
        for (String field : new String[] {customerExternalId, metricCode,
                quantity.stripTrailingZeros().toPlainString(), occurredAt.toString(),
                uniqueKey == null ? "" : uniqueKey}) {
            canonical.append(field.length()).append(':').append(field).append(';');
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
