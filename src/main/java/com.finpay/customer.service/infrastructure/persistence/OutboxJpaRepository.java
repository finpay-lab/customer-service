package com.finpay.customer.service.infrastructure.persistence;

import com.finpay.customer.service.domain.event.CustomerCreatedEvent;
import com.finpay.customer.service.domain.event.CustomerStatusChangedEvent;
import com.finpay.customer.service.domain.event.DomainEvent;
import com.finpay.customer.service.domain.event.KycStateChangedEvent;
import com.finpay.customer.service.domain.repository.OutboxRepository;
import com.finpay.customer.service.infrastructure.outbox.CustomerEventSerializer;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * JPA-backed transactional outbox. Domain events are appended inside the
 * application transaction; publication happens later by the relay.
 */
@Repository
public class OutboxJpaRepository implements OutboxRepository {

    private final OutboxSpringRepository springRepository;
    private final CustomerEventSerializer serializer;

    public OutboxJpaRepository(OutboxSpringRepository springRepository, CustomerEventSerializer serializer) {
        this.springRepository = springRepository;
        this.serializer = serializer;
    }

    @Override
    public void save(DomainEvent event) {
        OutboxEventJpaEntity entity = new OutboxEventJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setAggregateType("CUSTOMER");
        entity.setAggregateId(aggregateIdOf(event));
        entity.setEventType(event.eventType());
        entity.setPayload(serializer.serialize(event));
        entity.setCreatedAt(Instant.now());
        springRepository.save(entity);
    }

    @Override
    public List<OutboxRecord> findUnpublished(int limit) {
        return springRepository.findByPublishedAtIsNullOrderByCreatedAtAsc(PageRequest.of(0, limit)).stream()
                .map(e -> new OutboxRecord(e.getId(), e.getAggregateType(), e.getAggregateId(),
                        e.getEventType(), e.getPayload()))
                .toList();
    }

    @Override
    public void markPublished(UUID outboxId) {
        springRepository.findById(outboxId).ifPresent(e -> {
            e.setPublishedAt(Instant.now());
            springRepository.save(e);
        });
    }

    private UUID aggregateIdOf(DomainEvent event) {
        return switch (event) {
            case CustomerCreatedEvent e -> e.customerId();
            case CustomerStatusChangedEvent e -> e.customerId();
            case KycStateChangedEvent e -> e.customerId();
            default -> throw new IllegalArgumentException("Unknown domain event " + event);
        };
    }
}