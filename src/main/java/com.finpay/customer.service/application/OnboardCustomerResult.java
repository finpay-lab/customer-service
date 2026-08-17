package com.finpay.customer.service.application;

import java.time.Instant;
import java.util.UUID;

/** Result of an onboarding attempt; {@code created} distinguishes fresh vs replay. */
public record OnboardCustomerResult(
        UUID customerId,
        String status,
        String kycState,
        Instant createdAt,
        boolean created) {
}
