package com.finpay.customer.service.domain.event;

import java.time.Instant;
import java.util.Map;

/**
 * Normalized transaction event used by the RAG index. Pure domain value; no
 * infrastructure types. Built by the Kafka consumer (infrastructure) from the
 * event envelope on {@code finpay.ledger} / {@code finpay.transfer}.
 *
 * <p>Idempotency: {@link #eventId()} is the stable business id. Consumers
 * deduplicate on it (Rule 7); the OpenSearch store uses it as the document id,
 * so re-delivery is an idempotent upsert.
 *
 * @param eventId     globally unique event id (deduplication key)
 * @param eventType   which lifecycle event this is
 * @param customerId  Kafka record key; the customer the event belongs to
 * @param accountId   primary account involved (ledger account / transfer source)
 * @param occurredAt  business timestamp of the event
 * @param description human-readable rendering used for full-text retrieval
 * @param details     raw payload preserved for citation/debugging (not indexed)
 */
public record TransactionEvent(
        String eventId,
        EventType eventType,
        String customerId,
        String accountId,
        Instant occurredAt,
        String description,
        Map<String, Object> details) {
}