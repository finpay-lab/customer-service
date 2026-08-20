package com.finpay.customer.service.infrastructure.persistence;

import com.finpay.customer.service.domain.CustomerOutbox;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
@Transactional
public class CustomerOutboxImpl implements CustomerOutbox {
    private final CustomerOutboxJpaRepository outbox;
    public CustomerOutboxImpl(CustomerOutboxJpaRepository outbox) { this.outbox = outbox; }

    @Override
    public void stage(String eventType, String aggregateId, String payload) {
        CustomerOutboxEntity e = new CustomerOutboxEntity();
        e.setId(UUID.randomUUID().toString());
        e.setEventType(eventType);
        e.setAggregateId(aggregateId);
        e.setPayload(payload);
        e.setCreatedAt(Instant.now());
        e.setSent(false);
        outbox.save(e);
    }
}
