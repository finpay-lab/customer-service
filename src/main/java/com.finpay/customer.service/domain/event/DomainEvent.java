package com.finpay.customer.service.domain.event;

/**
 * Marker for domain events published to the transactional outbox. Event JSON
 * serialization to the `contracts/events/v1/*` shape happens in the
 * infrastructure layer, keeping the domain framework-free.
 */
public interface DomainEvent {
    String eventType();
}
