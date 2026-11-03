package io.github.joicepassos.billing.accounts;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.joicepassos.billing.ApiClient;
import io.github.joicepassos.billing.TestcontainersConfiguration;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** AC-03 (tenant isolation) and AC-04 (customer fiscal data validation). */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class CustomersIT {

    private static final Pattern ID = Pattern.compile("\"id\":\"([0-9a-f-]{36})\"");

    @LocalServerPort
    int port;

    @Autowired
    TenantProvisioning provisioning;

    @Autowired
    JdbcTemplate jdbc;

    ApiClient tenantA;
    ApiClient tenantB;

    @BeforeEach
    void tenants() {
        tenantA = new ApiClient(port, provisioning.provision("Tenant A").apiKey());
        tenantB = new ApiClient(port, provisioning.provision("Tenant B").apiKey());
    }

    @Test
    void createsACustomerAndReturnsMaskedPersonalData(CapturedOutput output) throws Exception {
        HttpResponse<String> response = tenantA.post("/v1/customers", customer("cus_1", "529.982.247-25"));

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.headers().firstValue("Location")).hasValueSatisfying(
                location -> assertThat(location).startsWith("/v1/customers/"));
        assertThat(response.body()).contains("\"externalId\":\"cus_1\"", "\"taxIdType\":\"CPF\"",
                "\"taxId\":\"***.982.247-**\"", "\"email\":\"a***@example.com\"")
                .doesNotContain("52998224725", "ana@example.com");
        String stored = jdbc.queryForObject("select tax_id from customer where external_id = 'cus_1'", String.class);
        assertThat(stored).isEqualTo("52998224725");
        assertThat(output.getAll()).doesNotContain("52998224725", "529.982.247-25", "ana@example.com");
    }

    @Test
    void acceptsAlphanumericCnpj() throws Exception {
        HttpResponse<String> response = tenantA.post("/v1/customers", customer("cus_2", "12.ABC.345/01DE-35"));

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).contains("\"taxIdType\":\"CNPJ\"", "\"taxId\":\"**.ABC.345/****-**\"");
    }

    @Test
    void invalidFiscalDataIsA422ListingTheFields() throws Exception {
        String body = """
                {"externalId":"cus_3","name":"Ana","taxId":"529.982.247-24","email":"ana@example.com",
                 "address":{"street":"Rua A","number":"10","district":"Centro","city":"Belo Horizonte",
                            "cityCode":"31062","state":"XX","postalCode":"30130-000"}}
                """;

        HttpResponse<String> response = tenantA.post("/v1/customers", body);

        assertThat(response.statusCode()).isEqualTo(422);
        assertThat(response.body()).contains("\"taxId\"", "\"address.cityCode\"", "\"address.state\"",
                "\"address.postalCode\"", "\"requestId\"");
        Integer stored = jdbc.queryForObject("select count(*) from customer where external_id = 'cus_3'",
                Integer.class);
        assertThat(stored).isZero();
    }

    @Test
    void missingAddressIsA422() throws Exception {
        String body = """
                {"externalId":"cus_4","name":"Ana","taxId":"52998224725","email":"ana@example.com"}
                """;

        HttpResponse<String> response = tenantA.post("/v1/customers", body);

        assertThat(response.statusCode()).isEqualTo(422);
        assertThat(response.body()).contains("\"address\"");
    }

    @Test
    void duplicateExternalIdIsA409WithinATenantButAllowedAcrossTenants() throws Exception {
        assertThat(tenantA.post("/v1/customers", customer("cus_5", "52998224725")).statusCode()).isEqualTo(201);

        assertThat(tenantA.post("/v1/customers", customer("cus_5", "52998224725")).statusCode()).isEqualTo(409);
        assertThat(tenantB.post("/v1/customers", customer("cus_5", "52998224725")).statusCode()).isEqualTo(201);
    }

    @Test
    void anotherTenantsCustomerIsNotFound() throws Exception {
        String id = idOf(tenantA.post("/v1/customers", customer("cus_6", "52998224725")));

        assertThat(tenantA.get("/v1/customers/" + id).statusCode()).isEqualTo(200);
        HttpResponse<String> fromOtherTenant = tenantB.get("/v1/customers/" + id);
        assertThat(fromOtherTenant.statusCode()).isEqualTo(404);
        assertThat(fromOtherTenant.headers().firstValue("Content-Type")).hasValueSatisfying(
                type -> assertThat(type).startsWith("application/problem+json"));
        assertThat(tenantB.patch("/v1/customers/" + id, "{\"name\":\"Hijacked\"}").statusCode()).isEqualTo(404);
        String name = jdbc.queryForObject("select name from customer where id = ?::uuid", String.class, id);
        assertThat(name).isEqualTo("Ana Souza");
    }

    @Test
    void patchUpdatesOnlyTheGivenFields() throws Exception {
        String id = idOf(tenantA.post("/v1/customers", customer("cus_7", "52998224725")));

        HttpResponse<String> response = tenantA.patch("/v1/customers/" + id,
                "{\"name\":\"Ana S. Lima\",\"taxId\":\"11.222.333/0001-81\"}");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"name\":\"Ana S. Lima\"", "\"taxIdType\":\"CNPJ\"",
                "\"email\":\"a***@example.com\"", "\"city\":\"Belo Horizonte\"");
        assertThat(tenantA.patch("/v1/customers/" + id, "{\"taxId\":\"123\"}").statusCode()).isEqualTo(422);
    }

    private static String customer(String externalId, String taxId) {
        return """
                {"externalId":"%s","name":"Ana Souza","taxId":"%s","email":"ana@example.com",
                 "address":{"street":"Avenida Afonso Pena","number":"1000","district":"Centro",
                            "city":"Belo Horizonte","cityCode":"3106200","state":"MG","postalCode":"30130003"}}
                """.formatted(externalId, taxId);
    }

    private static String idOf(HttpResponse<String> response) {
        assertThat(response.statusCode()).isEqualTo(201);
        Matcher matcher = ID.matcher(response.body());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
