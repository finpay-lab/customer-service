package com.finpay.customer.interfaces.web.dto;

import com.finpay.customer.domain.model.CustomerProfile;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Transport DTO for customer creation. Maps to a domain {@link CustomerProfile}.
 */
public record CreateCustomerRequest(
        @NotBlank @Size(max = 64) String firstName,
        @NotBlank @Size(max = 64) String lastName,
        @NotBlank @Email @Size(max = 254) String email,
        @Size(max = 32) String phone,
        @NotBlank @Size(min = 2, max = 2) String country
) {

    public CustomerProfile toProfile() {
        return new CustomerProfile(firstName, lastName, email, phone, country);
    }
}
