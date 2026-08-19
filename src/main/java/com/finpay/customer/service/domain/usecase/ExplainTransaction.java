package com.finpay.customer.service.domain.usecase;

import com.finpay.customer.service.domain.event.Explanation;
import com.finpay.customer.service.domain.event.RetrievedEvent;
import com.finpay.customer.service.domain.exception.LlmUnavailableException;
import com.finpay.customer.service.domain.port.LlmGateway;
import com.finpay.customer.service.domain.port.TransactionEventStore;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Explains a customer's transaction history in plain language (RAG):
 * retrieve relevant events for the customer/question, then have the LLM
 * synthesize an answer that cites the retrieved event ids.
 *
 * <p>Domain use case, free of Spring/Kafka/HTTP (Rule 3 / Rule 4). The
 * controller only maps transport to this class.
 *
 * <p>Degraded path: when the LLM is unavailable the answer is a deterministic
 * summary of the retrieved evidence — the endpoint never hard-fails because of
 * the AI provider. Search unavailability, however, propagates
 * (ExplainUnavailableException) because returning "no evidence" would be a
 * misleading answer.
 */
public final class ExplainTransaction {

    private final TransactionEventStore eventStore;
    private final LlmGateway llm;
    private final int maxEvidence;

    public ExplainTransaction(TransactionEventStore eventStore, LlmGateway llm, int maxEvidence) {
        this.eventStore = eventStore;
        this.llm = llm;
        this.maxEvidence = maxEvidence;
    }

    public Explanation explain(String customerId, String query) {
        List<RetrievedEvent> evidence = eventStore.search(customerId, query, maxEvidence);
        List<String> cited = evidence.stream().map(RetrievedEvent::eventId).toList();
        if (evidence.isEmpty()) {
            return new Explanation(customerId, query,
                    "No transaction events were found for this customer matching the question.",
                    cited, false);
        }
        String answer;
        try {
            answer = llm.synthesize(query, evidence);
        } catch (LlmUnavailableException e) {
            answer = fallbackAnswer(query, evidence);
        }
        return new Explanation(customerId, query, answer, cited, true);
    }

    private static String fallbackAnswer(String query, List<RetrievedEvent> evidence) {
        String events = evidence.stream()
                .map(event -> "- " + event.description())
                .collect(Collectors.joining("\n"));
        String ids = evidence.stream().map(RetrievedEvent::eventId).collect(Collectors.joining(", "));
        return """
                The AI assistant is currently unavailable. Based on the %d most relevant events for "%s":
                %s
                Referenced event IDs: %s"""
                .formatted(evidence.size(), query, events, ids);
    }
}