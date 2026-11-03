/**
 * Shared kernel: money, billing periods, time and HTTP error handling. Open to every module and free of business rules.
 */
@ApplicationModule(displayName = "Shared", type = ApplicationModule.Type.OPEN)
package io.github.joicepassos.billing.shared;

import org.springframework.modulith.ApplicationModule;
