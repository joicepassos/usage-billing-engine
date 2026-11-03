package io.github.joicepassos.billing.accounts.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/customers")
@SecurityRequirement(name = "apiKey")
class CustomerController {

    private final CustomerService customers;

    CustomerController(CustomerService customers) {
        this.customers = customers;
    }

    @PostMapping
    @Operation(summary = "Register a customer with the fiscal data needed to issue NFS-e")
    ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequests.CreateCustomer request) {
        Customer customer = customers.create(request);
        return ResponseEntity.created(URI.create("/v1/customers/" + customer.id())).body(CustomerResponse.of(customer));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a customer")
    CustomerResponse get(@PathVariable UUID id) {
        return CustomerResponse.of(customers.get(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update a customer; absent fields stay as they are")
    CustomerResponse update(@PathVariable UUID id, @Valid @RequestBody CustomerRequests.UpdateCustomer request) {
        return CustomerResponse.of(customers.update(id, request));
    }
}
