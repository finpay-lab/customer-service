package com.finpay.customer.application.port;

import com.finpay.customer.domain.event.DomainEvent;

/**
 * Application-side port for appending domain events to the transactional
 * outbox (ADR-0004). Implemented in {@code infrastructure/}.
 */
public interface OutboxAppender {

    void append(DomainEvent event);
}
