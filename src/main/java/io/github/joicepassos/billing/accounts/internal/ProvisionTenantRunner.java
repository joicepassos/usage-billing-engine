package io.github.joicepassos.billing.accounts.internal;

import io.github.joicepassos.billing.accounts.ProvisionedTenant;
import io.github.joicepassos.billing.accounts.TenantProvisioning;
import java.io.PrintStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Operator command: {@code ./gradlew bootRun --args='--provision-tenant="Acme"'}. Creates the tenant, prints the
 * API key once to standard output (never to the logs) and exits.
 */
@Component
class ProvisionTenantRunner implements ApplicationRunner {

    static final String OPTION = "provision-tenant";

    private final TenantProvisioning provisioning;
    private final ApplicationContext context;
    private final PrintStream out;

    @Autowired
    ProvisionTenantRunner(TenantProvisioning provisioning, ApplicationContext context) {
        this(provisioning, context, System.out);
    }

    ProvisionTenantRunner(TenantProvisioning provisioning, ApplicationContext context, PrintStream out) {
        this.provisioning = provisioning;
        this.context = context;
        this.out = out;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption(OPTION)) {
            return;
        }
        String name = String.join(" ", args.getOptionValues(OPTION));
        ProvisionedTenant tenant = provisioning.provision(name);
        out.println("Tenant created: " + tenant.name() + " (" + tenant.tenantId() + ")");
        out.println("API key (shown only once): " + tenant.apiKey());
        out.flush();
        System.exit(SpringApplication.exit(context, () -> 0));
    }
}
