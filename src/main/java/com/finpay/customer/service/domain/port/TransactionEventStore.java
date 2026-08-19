package com.finpay.customer.service.domain.port;

import com.finpay.customer.service.domain.event.RetrievedEvent;
import com.finpay.customer.service.domain.event.TransactionEvent;

import java.util.List;

/**
 * Read-model store for transaction events (OpenSearch, ADR-0010). Interface in
 * domain so the use case never depends on infrastructure; implementation lives
 * in infrastructure/.
 */
public interface TransactionEventStore {

    /**
     * Idempotently persists an event. Implementations MUST treat the eventId as
     * the identity (document id), so duplicate deliveries overwrite in place.
     * Best-effort: index failures must not break the Kafka stream.
     */
    void index(TransactionEvent event);

    /**
     * Retrieves the most relevant events for a customer and question.
     *
     * @throws com.finpay.customer.service.domain.exception.ExplainUnavailableException
     *         when the store cannot be reached
     */
    List<RetrievedEvent> search(String customerId, String query, int limit);
}