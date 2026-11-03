package io.github.joicepassos.billing.accounts.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CustomerResponseTest {

    @ParameterizedTest
    @CsvSource({
            "ana@example.com, a***@example.com",
            "a@example.com, ***@example.com"
    })
    void emailIsMasked(String email, String masked) {
        assertThat(CustomerResponse.maskEmail(email)).isEqualTo(masked);
    }
}
