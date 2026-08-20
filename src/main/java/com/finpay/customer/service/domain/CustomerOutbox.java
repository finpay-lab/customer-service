package com.finpay.customer.service.domain;

/** Transactional outbox port (Rule 5: persist+commit, then publish). */
public interface CustomerOutbox {
    void stage(String eventType, String aggregateId, String payload);
}
