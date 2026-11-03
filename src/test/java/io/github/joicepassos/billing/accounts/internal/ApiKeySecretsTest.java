package io.github.joicepassos.billing.accounts.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.MethodSource;

class ApiKeySecretsTest {

    private final ApiKeySecrets secrets = new ApiKeySecrets(new SecureRandom());

    @Test
    void generatedKeysHaveTheTestPrefixAndAreUnique() {
        String first = secrets.generate();
        String second = secrets.generate();

        assertThat(first).startsWith("sk_test_").hasSize("sk_test_".length() + 43);
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void lookupPrefixIsThePublicPartOnly() {
        String key = secrets.generate();

        assertThat(ApiKeySecrets.lookupPrefix(key)).hasValue(key.substring(0, 20));
    }

    @Test
    void theStoredHashIsNotTheKeyAndOnlyMatchesTheSameKey() {
        String key = secrets.generate();
        byte[] hash = ApiKeySecrets.hash(key);

        assertThat(hash).isNotEqualTo(key.getBytes(StandardCharsets.UTF_8)).hasSize(32);
        assertThat(ApiKeySecrets.matches(key, hash)).isTrue();
        assertThat(ApiKeySecrets.matches(secrets.generate(), hash)).isFalse();
    }

    @ParameterizedTest
    @NullSource
    @MethodSource("malformedKeys")
    void malformedKeysHaveNoLookupPrefix(String key) {
        assertThat(ApiKeySecrets.lookupPrefix(key)).isEmpty();
    }

    // Built at runtime so no key-shaped literal lives in the repository (secret scanners flag them).
    static Stream<String> malformedKeys() {
        String body = "A".repeat(43);
        return Stream.of("", "sk_test_", "sk_" + "live_" + body, "sk_test_short",
                "sk_test_" + body.substring(1) + "!");
    }
}
