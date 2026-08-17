package com.finpay.customer.service.domain.event;

import com.finpay.customer.service.domain.model.Customer;
import com.finpay.customer.service.domain.model.CustomerStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Published on `finpay.customer` when the customer lifecycle status changes
 * (EVENT_CATALOG: CustomerStatusChanged).
 */
public record CustomerStatusChangedEvent(
        UUID eventId,
        UUID customerId,
        CustomerStatus previousStatus,
        CustomerStatus newStatus,
        Instant ts) implements DomainEvent {

    public static CustomerStatusChangedEvent of(Customer customer, CustomerStatus previous) {
        return new CustomerStatusChangedEvent(
                UUID.randomUUID(), customer.id(), previous, customer.status(), Instant.now());
    }

    @Override
    public String eventType() {
        return "CustomerStatusChanged";
    }
}
