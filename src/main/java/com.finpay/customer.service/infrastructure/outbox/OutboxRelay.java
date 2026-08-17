package com.finpay.customer.service.infrastructure.outbox;

import com.finpay.customer.service.application.port.EventPublisher;
import com.finpay.customer.service.domain.repository.OutboxRepository;
import com.finpay.customer.service.domain.repository.OutboxRepository.OutboxRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Polls the transactional outbox and publishes pending rows after their
 * transaction committed. Publishing happens here — never inside the writing
 * transaction (AGENTS.md rule 5). On broker failure the row stays unpublished
 * and is retried on the next tick (at-least-once delivery, ADR-0002).
 */
@Component
@ConditionalOnProperty(name = "app.outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository outboxRepository;
    private final EventPublisher eventPublisher;
    private final String customerTopic;

    public OutboxRelay(OutboxRepository outboxRepository,
                       EventPublisher eventPublisher,
                       @Value("${finpay.customer.topic:finpay.customer}") String customerTopic) {
        this.outboxRepository = outboxRepository;
        this.eventPublisher = eventPublisher;
        this.customerTopic = customerTopic;
    }

    @Scheduled(fixedDelayString = "${app.outbox.relay.interval-ms:5000}")
    public void publishPending() {
        List<OutboxRecord> pending;
        try {
            pending = outboxRepository.findUnpublished(100);
        } catch (RuntimeException e) {
            log.warn("Outbox relay could not read pending rows", e);
            return;
        }
        for (OutboxRecord record : pending) {
            try {
                eventPublisher.publish(customerTopic, record.aggregateId().toString(), record.payload());
                outboxRepository.markPublished(record.id());
            } catch (RuntimeException e) {
                log.warn("Outbox publish failed for event {} ({}); will retry on next tick",
                        record.id(), record.eventType(), e);
            }
        }
    }
}