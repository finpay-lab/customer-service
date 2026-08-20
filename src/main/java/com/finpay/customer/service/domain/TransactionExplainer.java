package com.finpay.customer.service.domain;

import java.util.List;

/**
 * RAG over a customer's financial events (FP-58 / AI-1). Domain contract only.
 * Retrieval (OpenSearch) and LLM synthesis live in {@code infrastructure/}.
 * The use-case cites eventIds so answers are auditable (no hallucination).
 */
public final class TransactionExplainer {

    /** Port: retrieve a customer's indexed events for a query (OpenSearch). */
    public interface EventStore {
        List<CustomerEvent> retrieve(String customerId, String query);
    }

    /** Port: LLM synthesizes a plain-language answer from retrieved events. */
    public interface LlmSynthesizer {
        Answer synthesize(String query, List<CustomerEvent> events);
    }

    public record CustomerEvent(String eventId, String customerId, String type,
                                String payload, String occurredAt) {}

    public record Answer(String text, List<String> citedEventIds) {}

    private final EventStore store;
    private final LlmSynthesizer llm;

    public TransactionExplainer(EventStore store, LlmSynthesizer llm) {
        this.store = store;
        this.llm = llm;
    }

    /** Returns a cited, plain-language explanation (e.g. 'why was my transfer reversed?'). */
    public Answer explain(String customerId, String query) {
        List<CustomerEvent> events = store.retrieve(customerId, query);
        return llm.synthesize(query, events);
    }
}
