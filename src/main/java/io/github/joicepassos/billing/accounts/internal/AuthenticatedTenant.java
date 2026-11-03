package io.github.joicepassos.billing.accounts.internal;

import java.util.UUID;

/** Principal of an API request authenticated with a tenant key. */
record AuthenticatedTenant(UUID tenantId, UUID apiKeyId) {
}
