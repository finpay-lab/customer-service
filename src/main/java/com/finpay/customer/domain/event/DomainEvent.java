package com.finpay.customer.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * A domain event recorded by the {@code Customer} aggregate. Events are later
 * flushed to the transactional outbox by the application layer and published to
 * Kafka (ADR-0004). Pure domain type: no framework dependencies.
 */
public sealed interface DomainEvent permits CustomerCreated, CustomerKycChanged {

    /** Globally unique event id; consumers MUST deduplicate on it. */
    UUID eventId();

    /** Business aggregate (customer) the event belongs to. */
    UUID aggregateId();

    /** Fixed event type, matches the contract discriminator. */
    String eventType();

    /** UTC timestamp the event occurred. */
    Instant occurredAt();

    /** Kafka partition key (customer id) so events for one customer stay ordered. */
    default String partitionKey() {
        return aggregateId().toString();
    }
}
