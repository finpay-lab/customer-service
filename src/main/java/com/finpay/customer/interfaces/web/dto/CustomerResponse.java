package com.finpay.customer.interfaces.web.dto;

import com.finpay.customer.domain.model.Customer;
import com.finpay.customer.domain.model.CustomerStatus;

import java.time.Instant;
import java.util.UUID;

/** Read/write projection of the {@link Customer} aggregate. */
public record CustomerResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String country,
        CustomerStatus status,
        boolean kycVerified,
        Instant createdAt,
        Instant updatedAt
) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.id(),
                customer.profile().firstName(),
                customer.profile().lastName(),
                customer.profile().email(),
                customer.profile().phone(),
                customer.profile().country(),
                customer.status(),
                customer.isKycVerified(),
                customer.createdAt(),
                customer.updatedAt());
    }
}
