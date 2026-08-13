package com.finpay.customer.infrastructure.outbox;

import com.finpay.customer.infrastructure.persistence.OutboxEventEntity;
import com.finpay.customer.infrastructure.persistence.OutboxJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Polls unpublished outbox rows, publishes them to Kafka and marks them
 * published (ADR-0004). Runs outside the request transaction, so a broker
 * outage never blocks business writes: the backlog keeps growing and drains
 * when Kafka recovers.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int BATCH_SIZE = 100;

    private final OutboxJpaRepository outbox;
    private final DomainEventPublisher publisher;
    private final String customerTopic;

    public OutboxRelay(
            OutboxJpaRepository outbox,
            DomainEventPublisher publisher,
            @Value("${finpay.kafka.topic.customer:finpay.customer}") String customerTopic) {
        this.outbox = outbox;
        this.publisher = publisher;
        this.customerTopic = customerTopic;
    }

    @Scheduled(fixedDelayString = "${finpay.outbox.poll-interval-ms:5000}")
    public void publishPending() {
        List<OutboxEventEntity> pending = outbox.findByPublishedFalseOrderByCreatedAtAsc(
                PageRequest.of(0, BATCH_SIZE));
        if (pending.isEmpty()) {
            return;
        }
        log.debug("Publishing {} outbox event(s)", pending.size());
        for (OutboxEventEntity event : pending) {
            try {
                publisher.publish(event.getPayload(), event.getPartitionKey(), customerTopic);
                event.setPublished(true);
                event.setPublishedAt(Instant.now());
                outbox.save(event);
            } catch (RuntimeException ex) {
                log.warn("Outbox publish failed for event {} (will retry): {}",
                        event.getId(), ex.getMessage());
            }
        }
    }
}
