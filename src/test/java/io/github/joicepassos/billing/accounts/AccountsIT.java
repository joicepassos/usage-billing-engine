package io.github.joicepassos.billing.accounts;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.joicepassos.billing.TestcontainersConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** AC-01 and AC-02. AC-03 (cross-tenant 404) is covered with customers in T-06. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class AccountsIT {

    private final HttpClient http = HttpClient.newHttpClient();

    @LocalServerPort
    int port;

    @Autowired
    TenantProvisioning provisioning;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void provisioningShowsTheKeyOnceAndStoresOnlyItsHash() throws Exception {
        ProvisionedTenant tenant = provisioning.provision("Transcribe Co");

        assertThat(tenant.apiKey()).startsWith("sk_test_");
        assertThat(tenant.toString()).doesNotContain(tenant.apiKey());
        byte[] stored = jdbc.queryForObject("select secret_hash from api_key where tenant_id = ?", byte[].class,
                tenant.tenantId());
        assertThat(stored).isEqualTo(MessageDigest.getInstance("SHA-256")
                .digest(tenant.apiKey().getBytes(StandardCharsets.UTF_8)));
        Integer plaintextCopies = jdbc.queryForObject(
                "select count(*) from api_key where lookup_prefix = ? or encode(secret_hash, 'escape') = ?",
                Integer.class, tenant.apiKey(), tenant.apiKey());
        assertThat(plaintextCopies).isZero();
    }

    @Test
    void aValidKeyAuthenticatesAsItsTenantWithoutEchoingTheKey() throws Exception {
        ProvisionedTenant tenant = provisioning.provision("Acme Speech");

        HttpResponse<String> response = get("/v1/tenant", tenant.apiKey());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains(tenant.tenantId().toString(), "Acme Speech")
                .doesNotContain(tenant.apiKey());
    }

    @Test
    void missingKeyIsA401ProblemWithRequestId() throws Exception {
        HttpResponse<String> response = get("/v1/tenant", null);

        assertUnauthorizedProblem(response);
    }

    @Test
    void unknownOrMalformedKeysAreRejected() throws Exception {
        ProvisionedTenant tenant = provisioning.provision("Other Co");
        String forged = tenant.apiKey().substring(0, tenant.apiKey().length() - 1)
                + (tenant.apiKey().endsWith("A") ? "B" : "A");

        assertUnauthorizedProblem(get("/v1/tenant", forged));
        assertUnauthorizedProblem(get("/v1/tenant", "not-a-key"));
    }

    private void assertUnauthorizedProblem(HttpResponse<String> response) {
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
                type -> assertThat(type).startsWith("application/problem+json"));
        String requestId = response.headers().firstValue("X-Request-Id").orElseThrow();
        assertThat(response.body()).contains("\"status\":401", "\"requestId\":\"" + requestId + "\"");
    }

    private HttpResponse<String> get(String path, String apiKey) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (apiKey != null) {
            request.header("Authorization", "Bearer " + apiKey);
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
