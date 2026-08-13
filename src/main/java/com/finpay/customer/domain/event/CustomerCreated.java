package com.finpay.customer.domain.event;

import com.finpay.customer.domain.model.Customer;
import com.finpay.customer.domain.model.CustomerStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Published on the {@code finpay.customer} topic when a customer profile is
 * created. Schema v1: contracts/events/v1/CustomerCreated.json.
 */
public record CustomerCreated(
        UUID eventId,
        UUID customerId,
        CustomerStatus status,
        boolean kycVerified,
        Instant occurredAt
) implements DomainEvent {

    @Override
    public UUID aggregateId() {
        return customerId;
    }

    @Override
    public String eventType() {
        return "CustomerCreated";
    }

    public static CustomerCreated of(Customer customer) {
        return new CustomerCreated(
                UUID.randomUUID(),
                customer.id(),
                customer.status(),
                customer.isKycVerified(),
                customer.updatedAt());
    }
}
