package io.github.joicepassos.billing.accounts.internal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * API key format and hashing. A key is {@code sk_test_<lookup><secret>}: 32 random bytes in URL-safe Base64
 * (43 characters). The first 12 characters after the prefix are stored in clear to find the key; the whole key is
 * stored only as a SHA-256 hash and compared in constant time. Keys are high-entropy, so a fast hash is enough.
 */
final class ApiKeySecrets {

    static final String PREFIX = "sk_test_";
    static final int LOOKUP_LENGTH = 12;
    private static final Pattern FORMAT = Pattern.compile("sk_test_[A-Za-z0-9_-]{43}");

    private final SecureRandom random;

    ApiKeySecrets(SecureRandom random) {
        this.random = random;
    }

    String generate() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static Optional<String> lookupPrefix(String key) {
        if (key == null || !FORMAT.matcher(key).matches()) {
            return Optional.empty();
        }
        return Optional.of(key.substring(0, PREFIX.length() + LOOKUP_LENGTH));
    }

    static byte[] hash(String key) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    static boolean matches(String key, byte[] storedHash) {
        return MessageDigest.isEqual(hash(key), storedHash);
    }
}
