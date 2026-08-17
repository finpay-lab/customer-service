package com.finpay.customer.service.domain.event;

import com.finpay.customer.service.domain.model.Customer;
import com.finpay.customer.service.domain.model.KycState;

import java.time.Instant;
import java.util.UUID;

/**
 * Published on `finpay.customer` when the KYC verification state changes
 * (EVENT_CATALOG: KycStateChanged).
 */
public record KycStateChangedEvent(
        UUID eventId,
        UUID customerId,
        KycState previousState,
        KycState newState,
        Instant ts) implements DomainEvent {

    public static KycStateChangedEvent of(Customer customer, KycState previous) {
        return new KycStateChangedEvent(
                UUID.randomUUID(), customer.id(), previous, customer.kycState(), Instant.now());
    }

    @Override
    public String eventType() {
        return "KycStateChanged";
    }
}
