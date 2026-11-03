package io.github.joicepassos.billing.accounts.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/tenant")
class TenantController {

    private final AccountsService accounts;

    TenantController(AccountsService accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    @Operation(summary = "The tenant that owns the API key", security = @SecurityRequirement(name = "apiKey"))
    TenantResponse current(@AuthenticationPrincipal AuthenticatedTenant principal) {
        Tenant tenant = accounts.find(principal.tenantId()).orElseThrow();
        return new TenantResponse(tenant.id(), tenant.name());
    }

    record TenantResponse(UUID id, String name) {
    }
}
