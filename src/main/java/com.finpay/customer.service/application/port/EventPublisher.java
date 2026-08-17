package com.finpay.customer.service.application.port;

/**
 * Outbound event publisher (infrastructure port). Implementations ship the
 * outbox payload onto Kafka; invoked only by the outbox relay after the
 * surrounding transaction has committed (AGENTS.md rule 5).
 */
public interface EventPublisher {

    /** Publishes {@code payloadJson} to {@code topic}, partitioned by {@code partitionKey}. */
    void publish(String topic, String partitionKey, String payloadJson);
}
