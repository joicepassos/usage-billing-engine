package io.github.joicepassos.billing.accounts.internal;

import io.github.joicepassos.billing.shared.taxid.ValidTaxId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request bodies for customers. {@code toString} never prints personal data. */
final class CustomerRequests {

    private CustomerRequests() {
    }

    record AddressRequest(
            @NotBlank @Size(max = 200) String street,
            @NotBlank @Size(max = 20) String number,
            @Size(max = 100) String complement,
            @NotBlank @Size(max = 100) String district,
            @NotBlank @Size(max = 100) String city,
            @NotBlank @Pattern(regexp = "\\d{7}", message = "must be the 7-digit IBGE municipality code") String cityCode,
            @NotBlank @Pattern(regexp = "AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO",
                    message = "must be a Brazilian state code (UF)") String state,
            @NotBlank @Pattern(regexp = "\\d{8}", message = "must be 8 digits (CEP)") String postalCode) {

        Customer.Address toAddress() {
            return new Customer.Address(street.strip(), number.strip(), complement == null ? null : complement.strip(),
                    district.strip(), city.strip(), cityCode, state, postalCode);
        }

        @Override
        public String toString() {
            return "AddressRequest[city=" + city + ", state=" + state + "]";
        }
    }

    record CreateCustomer(
            @NotBlank @Size(max = 100) @Pattern(regexp = "[A-Za-z0-9._:-]+",
                    message = "may contain only letters, digits and . _ : -") String externalId,
            @NotBlank @Size(max = 200) String name,
            @NotBlank @ValidTaxId String taxId,
            @NotBlank @Email @Size(max = 200) String email,
            @NotNull @Valid AddressRequest address) {

        @Override
        public String toString() {
            return "CreateCustomer[externalId=" + externalId + "]";
        }
    }

    /** Partial update: absent fields stay as they are. The address, when present, is replaced as a whole. */
    record UpdateCustomer(
            @Size(min = 1, max = 200) String name,
            @ValidTaxId String taxId,
            @Email @Size(max = 200) String email,
            @Valid AddressRequest address) {

        @Override
        public String toString() {
            return "UpdateCustomer[]";
        }
    }
}
