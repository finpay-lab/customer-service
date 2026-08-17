package com.finpay.customer.service.testutil;

import com.finpay.customer.service.domain.event.DomainEvent;
import com.finpay.customer.service.domain.repository.OutboxRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** In-memory outbox for unit tests. */
public class FakeOutboxRepository implements OutboxRepository {

    private final List<OutboxRecord> records = new ArrayList<>();
    private final List<DomainEvent> events = new ArrayList<>();

    @Override
    public void save(DomainEvent event) {
        events.add(event);
        records.add(new OutboxRecord(UUID.randomUUID(), "CUSTOMER", UUID.randomUUID(),
                event.eventType(), event.toString()));
    }

    @Override
    public List<OutboxRecord> findUnpublished(int limit) {
        return List.copyOf(records);
    }

    @Override
    public void markPublished(UUID outboxId) {
        // no-op for unit tests
    }

    public List<DomainEvent> events() {
        return events;
    }
}