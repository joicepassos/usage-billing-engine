package io.github.joicepassos.billing.accounts.internal;

import java.util.UUID;

/** Tax id and email are returned masked: the tenant already has them, and responses travel through logs and proxies. */
record CustomerResponse(UUID id, String externalId, String name, String taxIdType, String taxId, String email,
        AddressResponse address) {

    record AddressResponse(String city, String cityCode, String state) {
    }

    static CustomerResponse of(Customer customer) {
        Customer.Address address = customer.address();
        return new CustomerResponse(customer.id(), customer.externalId(), customer.name(),
                customer.taxId().type().name(), customer.taxId().masked(), maskEmail(customer.email()),
                new AddressResponse(address.city(), address.cityCode(), address.state()));
    }

    static String maskEmail(String email) {
        int at = email.indexOf('@');
        return at <= 1 ? "***" + email.substring(Math.max(at, 0)) : email.charAt(0) + "***" + email.substring(at);
    }
}
