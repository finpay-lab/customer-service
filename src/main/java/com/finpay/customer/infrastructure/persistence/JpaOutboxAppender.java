package com.finpay.customer.infrastructure.persistence;

import com.finpay.customer.application.port.OutboxAppender;
import com.finpay.customer.domain.event.DomainEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Appends a domain event to the outbox. Called from within the same transaction
 * as the business change (ADR-0004): persist + commit, then publish.
 */
@Component
public class JpaOutboxAppender implements OutboxAppender {

    private final OutboxJpaRepository outbox;
    private final DomainEventSerializer serializer;

    public JpaOutboxAppender(OutboxJpaRepository outbox, DomainEventSerializer serializer) {
        this.outbox = outbox;
        this.serializer = serializer;
    }

    @Override
    public void append(DomainEvent event) {
        OutboxEventEntity entity = new OutboxEventEntity();
        entity.setId(event.eventId());
        entity.setAggregateId(event.aggregateId());
        entity.setEventType(event.eventType());
        entity.setPartitionKey(event.partitionKey());
        entity.setPayload(serializer.serialize(event));
        entity.setPublished(false);
        entity.setCreatedAt(Instant.now());
        outbox.save(entity);
    }
}
