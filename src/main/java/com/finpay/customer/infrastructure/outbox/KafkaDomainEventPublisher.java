package com.finpay.customer.infrastructure.outbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka-backed {@link DomainEventPublisher}. Uses a blocking send so broker
 * failures surface synchronously and the outbox row is left unpublished for
 * retry; this is the pairing that makes at-least-once delivery correct.
 */
@Component
public class KafkaDomainEventPublisher implements DomainEventPublisher {

    private final KafkaTemplate<String, String> kafka;
    private final String customerTopic;

    public KafkaDomainEventPublisher(
            KafkaTemplate<String, String> kafka,
            @Value("${finpay.kafka.topic.customer:finpay.customer}") String customerTopic) {
        this.kafka = kafka;
        this.customerTopic = customerTopic;
    }

    @Override
    public void publish(String payload, String partitionKey, String topic) {
        try {
            kafka.send(topic, partitionKey, payload).get(10, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Kafka publish failed for partitionKey " + partitionKey, ex);
        }
    }

    /** Convenience for the customer stream (the only stream this service owns). */
    public void publishCustomerEvent(String payload, String partitionKey) {
        publish(payload, partitionKey, customerTopic);
    }
}
