package com.finpay.customer.infrastructure.outbox;

/**
 * Publishes a serialized event to Kafka (at-least-once). Failures are thrown
 * and the outbox row stays unpublished so the relay retries (ADR-0004).
 */
public interface DomainEventPublisher {

    void publish(String payload, String partitionKey, String topic);
}
