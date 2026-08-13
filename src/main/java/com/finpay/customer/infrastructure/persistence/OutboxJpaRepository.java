package com.finpay.customer.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data access for the {@code outbox_events} table. */
public interface OutboxJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {

    /** Unpublished rows in insertion order (backlog when Kafka is down). */
    List<OutboxEventEntity> findByPublishedFalseOrderByCreatedAtAsc(Pageable pageable);
}
