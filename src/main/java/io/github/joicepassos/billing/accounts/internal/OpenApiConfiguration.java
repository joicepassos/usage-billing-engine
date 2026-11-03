package io.github.joicepassos.billing.accounts.internal;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

    @Bean
    OpenAPI usageBillingOpenApi() {
        return new OpenAPI()
                .info(new Info().title("usage-billing-engine API").version("v1")
                        .license(new License().name("MIT")))
                .components(new Components().addSecuritySchemes("apiKey", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer")
                        .description("Tenant API key, e.g. `Authorization: Bearer sk_test_...`")));
    }
}
