package io.github.joicepassos.billing.metering;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.joicepassos.billing.ApiClient;
import io.github.joicepassos.billing.TestcontainersConfiguration;
import io.github.joicepassos.billing.accounts.TenantProvisioning;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** AC-10, AC-11, AC-12, plus business rejections and tenant isolation of ingestion. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class EventIngestionIT {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    @LocalServerPort
    int port;

    @Autowired
    TenantProvisioning provisioning;

    @Autowired
    JdbcTemplate jdbc;

    ApiClient api;
    UUID tenantId;
    String now;
    String currentPeriod;

    @BeforeEach
    void tenantWithMetrics() throws Exception {
        var tenant = provisioning.provision("Transcribe Co");
        tenantId = tenant.tenantId();
        api = new ApiClient(port, tenant.apiKey());
        assertThat(api.post("/v1/metrics", "{\"code\":\"minutes\",\"name\":\"Transcribed minutes\",\"aggregation\":\"sum\"}")
                .statusCode()).isEqualTo(201);
        assertThat(api.post("/v1/metrics", "{\"code\":\"speakers\",\"name\":\"Distinct speakers\",\"aggregation\":\"unique\"}")
                .statusCode()).isEqualTo(201);
        Instant instant = Instant.now().minus(1, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);
        now = instant.toString();
        currentPeriod = YearMonth.from(instant.atZone(SAO_PAULO)).toString();
    }

    @Test
    void aBatchOf500IsStoredAndAcknowledgedWith202() throws Exception {
        String batch = batch(IntStream.range(0, 500).mapToObj(i -> event("tx-" + i, "cus_" + (i % 7), "minutes", "1.5", now)).toList());

        long started = System.nanoTime();
        HttpResponse<String> response = api.post("/v1/events", batch);
        long millis = (System.nanoTime() - started) / 1_000_000;

        assertThat(response.statusCode()).isEqualTo(202);
        assertThat(response.body()).contains("\"accepted\":500", "\"duplicates\":0", "\"conflicts\":0", "\"rejected\":0");
        assertThat(countEvents()).isEqualTo(500);
        assertThat(jdbc.queryForObject("select sum(quantity) from usage_event where tenant_id = ?",
                java.math.BigDecimal.class, tenantId)).isEqualByComparingTo("750");
        assertThat(jdbc.queryForObject("select count(distinct billing_period) from usage_event where tenant_id = ?",
                Integer.class, tenantId)).isEqualTo(1);
        // AC-10 target (< 500 ms p95) is measured, not asserted here: CI timing is too noisy to gate on.
        System.out.println("AC-10 measurement: 500-event batch acknowledged in " + millis + " ms (first request, cold)");
    }

    @Test
    void aRetriedBatchIs200WithTheOriginalResultAndChangesNothing() throws Exception {
        String batch = batch(List.of(event("tx-1", "cus_1", "minutes", "3", now), event("tx-2", "cus_1", "minutes", "4", now)));
        assertThat(api.post("/v1/events", batch).statusCode()).isEqualTo(202);

        HttpResponse<String> retry = api.post("/v1/events", batch);

        assertThat(retry.statusCode()).isEqualTo(200);
        assertThat(retry.body()).contains("\"accepted\":0", "\"duplicates\":2",
                "\"status\":\"duplicate\"", "\"billingPeriod\":\"" + currentPeriod + "\"", "\"occurredAt\":\"" + now + "\"");
        assertThat(countEvents()).isEqualTo(2);
    }

    @Test
    void aConflictingPayloadIs409ForThatEventAndTheRestIsProcessed() throws Exception {
        api.post("/v1/events", batch(List.of(event("tx-1", "cus_1", "minutes", "3", now))));

        HttpResponse<String> response = api.post("/v1/events", batch(List.of(
                event("tx-1", "cus_1", "minutes", "30", now),
                event("tx-2", "cus_1", "minutes", "5", now))));

        assertThat(response.statusCode()).isEqualTo(202);
        // tx-2 is the accepted one, so the single conflict is tx-1.
        assertThat(response.body()).contains("\"accepted\":1", "\"conflicts\":1", "\"status\":\"conflict\"",
                "\"status\":409", "different payload");
        assertThat(jdbc.queryForObject("select quantity from usage_event where tenant_id = ? and transaction_id = 'tx-1'",
                java.math.BigDecimal.class, tenantId)).isEqualByComparingTo("3");
        assertThat(countEvents()).isEqualTo(2);
    }

    @Test
    void repeatsInsideOneBatchAreJudgedAgainstTheFirstOccurrence() throws Exception {
        HttpResponse<String> response = api.post("/v1/events", batch(List.of(
                event("tx-1", "cus_1", "minutes", "1", now),
                event("tx-1", "cus_1", "minutes", "1", now),
                event("tx-1", "cus_1", "minutes", "2", now))));

        assertThat(response.statusCode()).isEqualTo(202);
        assertThat(response.body()).contains("\"accepted\":1", "\"duplicates\":1", "\"conflicts\":1");
        assertThat(countEvents()).isEqualTo(1);
    }

    @Test
    void businessProblemsRejectOnlyTheEventConcerned() throws Exception {
        String future = Instant.now().plus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS).toString();

        HttpResponse<String> response = api.post("/v1/events", batch(List.of(
                event("tx-1", "cus_1", "calls", "1", now),
                event("tx-2", "cus_1", "speakers", "1", now),
                event("tx-3", "cus_1", "minutes", "1", future),
                uniqueEvent("tx-4", "cus_1", "speaker-a", now))));

        assertThat(response.statusCode()).isEqualTo(202);
        assertThat(response.body()).contains("\"accepted\":1", "\"rejected\":3",
                "Unknown metric 'calls'", "uniqueKey is required", "more than 5 minutes in the future");
        assertThat(countEvents()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select unique_key from usage_event where tenant_id = ?", String.class, tenantId))
                .isEqualTo("speaker-a");
    }

    @Test
    void unknownCustomersAreAcceptedAndKept() throws Exception {
        HttpResponse<String> response = api.post("/v1/events", batch(List.of(event("tx-1", "never_registered", "minutes", "1", now))));

        assertThat(response.statusCode()).isEqualTo(202);
        assertThat(countEvents()).isEqualTo(1);
    }

    @Test
    void schemaErrorsRejectTheWholeBatchWith422() throws Exception {
        String tooMany = batch(IntStream.range(0, 501).mapToObj(i -> event("tx-" + i, "cus_1", "minutes", "1", now)).toList());
        String negative = batch(List.of(event("tx-ok", "cus_1", "minutes", "1", now), event("tx-bad", "cus_1", "minutes", "-1", now)));

        HttpResponse<String> tooManyResponse = api.post("/v1/events", tooMany);
        HttpResponse<String> negativeResponse = api.post("/v1/events", negative);
        HttpResponse<String> emptyResponse = api.post("/v1/events", "{\"events\":[]}");

        assertThat(tooManyResponse.statusCode()).isEqualTo(422);
        assertThat(negativeResponse.statusCode()).isEqualTo(422);
        assertThat(negativeResponse.body()).contains("events[1].quantity");
        assertThat(emptyResponse.statusCode()).isEqualTo(422);
        assertThat(countEvents()).isZero();
    }

    @Test
    void transactionIdsAndMetricsAreScopedToTheTenant() throws Exception {
        var other = provisioning.provision("Other Co");
        ApiClient otherApi = new ApiClient(port, other.apiKey());
        api.post("/v1/events", batch(List.of(event("tx-1", "cus_1", "minutes", "1", now))));

        HttpResponse<String> otherWithoutMetric = otherApi.post("/v1/events", batch(List.of(event("tx-1", "cus_1", "minutes", "1", now))));
        assertThat(otherWithoutMetric.body()).contains("Unknown metric 'minutes'");
        assertThat(otherApi.get("/v1/metrics").body()).isEqualTo("[]");

        otherApi.post("/v1/metrics", "{\"code\":\"minutes\",\"name\":\"Minutes\",\"aggregation\":\"sum\"}");
        HttpResponse<String> otherAccepted = otherApi.post("/v1/events", batch(List.of(event("tx-1", "cus_1", "minutes", "1", now))));
        assertThat(otherAccepted.statusCode()).isEqualTo(202);
        assertThat(countEvents()).isEqualTo(1);
    }

    @Test
    void metricCodesAreUniquePerTenantAndValidated() throws Exception {
        assertThat(api.post("/v1/metrics", "{\"code\":\"minutes\",\"name\":\"Again\",\"aggregation\":\"sum\"}").statusCode())
                .isEqualTo(409);
        HttpResponse<String> invalid = api.post("/v1/metrics", "{\"code\":\"Bad Code\",\"name\":\"x\",\"aggregation\":\"avg\"}");
        assertThat(invalid.statusCode()).isEqualTo(422);
        assertThat(invalid.body()).contains("\"code\"", "\"aggregation\"");
        assertThat(api.get("/v1/metrics").body()).contains("\"code\":\"minutes\"", "\"aggregation\":\"sum\"",
                "\"code\":\"speakers\"", "\"aggregation\":\"unique\"");
    }

    private int countEvents() {
        return jdbc.queryForObject("select count(*) from usage_event where tenant_id = ?", Integer.class, tenantId);
    }

    private static String event(String tx, String customer, String metric, String quantity, String occurredAt) {
        return "{\"transactionId\":\"%s\",\"customerExternalId\":\"%s\",\"metricCode\":\"%s\",\"quantity\":%s,\"occurredAt\":\"%s\"}"
                .formatted(tx, customer, metric, quantity, occurredAt);
    }

    private static String uniqueEvent(String tx, String customer, String uniqueKey, String occurredAt) {
        return "{\"transactionId\":\"%s\",\"customerExternalId\":\"%s\",\"metricCode\":\"speakers\",\"quantity\":1,\"occurredAt\":\"%s\",\"uniqueKey\":\"%s\"}"
                .formatted(tx, customer, occurredAt, uniqueKey);
    }

    private static String batch(List<String> events) {
        return "{\"events\":[" + String.join(",", events) + "]}";
    }
}
