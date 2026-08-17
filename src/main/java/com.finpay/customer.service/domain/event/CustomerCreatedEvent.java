package com.finpay.customer.service.domain.event;

import com.finpay.customer.service.domain.model.Customer;
import com.finpay.customer.service.domain.model.CustomerStatus;
import com.finpay.customer.service.domain.model.KycState;

import java.time.Instant;
import java.util.UUID;

/**
 * Published on `finpay.customer` when a customer profile is created.
 * Contract: contracts/events/v1/CustomerCreated.json.
 */
public record CustomerCreatedEvent(
        UUID eventId,
        UUID customerId,
        CustomerStatus status,
        KycState kycState,
        Instant ts) implements DomainEvent {

    public static CustomerCreatedEvent of(Customer customer) {
        return new CustomerCreatedEvent(
                UUID.randomUUID(), customer.id(), customer.status(), customer.kycState(), Instant.now());
    }

    @Override
    public String eventType() {
        return "CustomerCreated";
    }
}
