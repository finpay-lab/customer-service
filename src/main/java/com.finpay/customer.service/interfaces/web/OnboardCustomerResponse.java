package com.finpay.customer.service.interfaces.web;

import java.time.Instant;
import java.util.UUID;

public record OnboardCustomerResponse(
        UUID customerId,
        String status,
        String kycState,
        Instant createdAt) {
}