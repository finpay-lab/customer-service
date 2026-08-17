package com.finpay.customer.service.infrastructure.outbox;

import com.finpay.customer.service.application.port.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Kafka-backed {@link EventPublisher}. A send must be acknowledged within a
 * bounded timeout (AGENTS.md rule 8); on failure it throws so the outbox relay
 * leaves the row unpublished for the next retry tick (at-least-once, ADR-0002).
 */
@Component
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Duration sendTimeout;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                               @Value("${finpay.customer.kafka.send-timeout-ms:5000}") long sendTimeoutMs) {
        this.kafkaTemplate = kafkaTemplate;
        this.sendTimeout = Duration.ofMillis(sendTimeoutMs);
    }

    @Override
    public void publish(String topic, String partitionKey, String payloadJson) {
        try {
            kafkaTemplate.send(topic, partitionKey, payloadJson)
                    .get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
            log.debug("Published outbox event to {} [key={}]", topic, partitionKey);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EventPublishException("Interrupted while publishing to " + topic, e);
        } catch (ExecutionException | TimeoutException e) {
            throw new EventPublishException("Failed to publish to " + topic + " within " + sendTimeout, e);
        }
    }
}