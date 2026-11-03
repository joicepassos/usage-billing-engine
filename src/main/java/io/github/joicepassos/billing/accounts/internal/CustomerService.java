package io.github.joicepassos.billing.accounts.internal;

import io.github.joicepassos.billing.shared.taxid.TaxId;
import io.github.joicepassos.billing.shared.web.ConflictException;
import io.github.joicepassos.billing.shared.web.ResourceNotFoundException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CustomerService {

    private final CustomerRepository customers;
    private final Clock clock;

    CustomerService(CustomerRepository customers, Clock clock) {
        this.customers = customers;
        this.clock = clock;
    }

    @Transactional
    Customer create(CustomerRequests.CreateCustomer request) {
        if (customers.existsByExternalId(request.externalId())) {
            throw duplicate(request.externalId());
        }
        Customer customer = new Customer(UUID.randomUUID(), request.externalId(), request.name().strip(),
                taxId(request.taxId()), request.email().strip(), request.address().toAddress(), clock.instant());
        try {
            return customers.saveAndFlush(customer);
        } catch (DataIntegrityViolationException e) {
            throw duplicate(request.externalId());
        }
    }

    @Transactional(readOnly = true)
    Customer get(UUID id) {
        return customers.findOneById(id).orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    @Transactional
    Customer update(UUID id, CustomerRequests.UpdateCustomer request) {
        Customer customer = get(id);
        customer.update(
                request.name() != null ? request.name().strip() : customer.name(),
                request.taxId() != null ? taxId(request.taxId()) : customer.taxId(),
                request.email() != null ? request.email().strip() : customer.email(),
                request.address() != null ? request.address().toAddress() : customer.address(),
                clock.instant());
        return customer;
    }

    private static TaxId taxId(String raw) {
        return TaxId.parse(raw).orElseThrow(() -> new IllegalStateException("Tax id passed validation but not parsing"));
    }

    private static ConflictException duplicate(String externalId) {
        return new ConflictException("A customer with externalId '" + externalId + "' already exists");
    }
}
