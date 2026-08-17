package com.finpay.customer.service.domain.repository;

import com.finpay.customer.service.domain.event.DomainEvent;

import java.util.List;
import java.util.UUID;

/**
 * Transactional outbox (AGENTS.md rules 2 and 5, ADR-0004). Domain events are
 * appended in the same transaction as the aggregate write and published
 * afterwards by the outbox relay — never inside the transaction.
 */
public interface OutboxRepository {

    /** Appends a domain event to the outbox (same transaction as the write). */
    void save(DomainEvent event);

    /** Batches of unpublished rows, oldest first, for the relay. */
    List<OutboxRecord> findUnpublished(int limit);

    void markPublished(UUID outboxId);

    /** A pending outbox row, decoupled from any JPA entity. */
    record OutboxRecord(
            UUID id,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            String payload) {
    }
}
