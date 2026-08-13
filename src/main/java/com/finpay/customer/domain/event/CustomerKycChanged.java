package com.finpay.customer.domain.event;

import com.finpay.customer.domain.model.Customer;
import com.finpay.customer.domain.model.CustomerStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Published on the {@code finpay.customer} topic whenever the customer's KYC
 * verification flag changes (approval or revocation), always together with the
 * resulting status (KYC_PENDING &lt;-&gt; ACTIVE).
 */
public record CustomerKycChanged(
        UUID eventId,
        UUID customerId,
        boolean kycVerified,
        CustomerStatus status,
        Instant occurredAt
) implements DomainEvent {

    @Override
    public UUID aggregateId() {
        return customerId;
    }

    @Override
    public String eventType() {
        return "CustomerKycChanged";
    }

    public static CustomerKycChanged of(Customer customer, Instant occurredAt) {
        return new CustomerKycChanged(
                UUID.randomUUID(),
                customer.id(),
                customer.isKycVerified(),
                customer.status(),
                occurredAt);
    }
}
