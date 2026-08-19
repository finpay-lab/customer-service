package com.finpay.customer.service.infrastructure.opensearch;

import com.finpay.customer.service.domain.event.EventType;
import com.finpay.customer.service.domain.event.RetrievedEvent;
import com.finpay.customer.service.domain.event.TransactionEvent;
import com.finpay.customer.service.domain.exception.ExplainUnavailableException;
import com.finpay.customer.service.domain.port.TransactionEventStore;
import com.finpay.customer.service.infrastructure.resilience.CircuitBreaker;
import com.finpay.customer.service.infrastructure.resilience.CircuitOpenException;
import com.finpay.customer.service.infrastructure.resilience.Retry;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenSearch read-model for transaction events (ADR-0010). Raw REST via Spring
 * RestClient so the OpenSearch Java client stays out of the build.
 *
 * <p>Idempotency (Rule 7): the document id equals {@code eventId}, so duplicate
 * deliveries are idempotent upserts. Indexing is best-effort — a failing search
 * index must never break the event stream (ADR-0010). Search, however, raises
 * {@link ExplainUnavailableException} so callers never get a misleading
 * "no evidence" answer when the index is down.
 */
public final class OpenSearchTransactionEventStore implements TransactionEventStore {

    private static final Logger LOG = LoggerFactory.getLogger(OpenSearchTransactionEventStore.class);

    private final RestClient client;
    private final String indexName;
    private final CircuitBreaker circuitBreaker;
    private final int maxRetries;
    private final Duration retryBackoff;

    public OpenSearchTransactionEventStore(RestClient client, String indexName,
                                           CircuitBreaker circuitBreaker, int maxRetries, Duration retryBackoff) {
        this.client = client;
        this.indexName = indexName;
        this.circuitBreaker = circuitBreaker;
        this.maxRetries = maxRetries;
        this.retryBackoff = retryBackoff;
    }

    @Override
    public void index(TransactionEvent event) {
        try {
            circuitBreaker.execute(() ->
                    Retry.withBackoff(maxRetries, retryBackoff, () -> doIndex(event)));
        } catch (RuntimeException e) {
            LOG.warn("Failed to index transaction event {} into OpenSearch: {}", event.eventId(), e.getMessage());
        }
    }

    @Override
    public List<RetrievedEvent> search(String customerId, String query, int limit) {
        try {
            return circuitBreaker.execute(() ->
                    Retry.withBackoff(maxRetries, retryBackoff, () -> doSearch(customerId, query, limit)));
        } catch (CircuitOpenException | RuntimeException e) {
            throw new ExplainUnavailableException("Transaction search index unavailable", e);
        }
    }

    /** Best-effort index creation on startup (versioned index name = mapping evolution, ADR-0010). */
    public void createIndexIfMissing() {
        boolean exists;
        try {
            exists = client.head().uri("/{index}", indexName).retrieve()
                    .toBodilessEntity().getStatusCode().is2xxSuccessful();
        } catch (RestClientResponseException e) {
            exists = false;
        }
        if (exists) {
            return;
        }
        try {
            client.put().uri("/{index}", indexName)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(indexMapping())
                    .retrieve().toBodilessEntity();
            LOG.info("Created OpenSearch index {}", indexName);
        } catch (RestClientResponseException e) {
            // 400 resource_already_exists is fine if creation raced another instance.
            if (e.getStatusCode().value() != 400) {
                LOG.warn("Could not create OpenSearch index {} (best-effort, ADR-0010): {}", indexName, e.getMessage());
            }
        }
    }

    /** The OpenSearch document id — always the eventId so redelivery is an idempotent upsert. */
    public static String documentId(TransactionEvent event) {
        return event.eventId();
    }

    public static Map<String, Object> documentBody(TransactionEvent event) {
        Map<String, Object> body = new HashMap<>();
        body.put("eventId", event.eventId());
        body.put("eventType", event.eventType().streamType());
        body.put("customerId", event.customerId());
        body.put("accountId", event.accountId());
        body.put("occurredAt", event.occurredAt());
        body.put("description", event.description());
        body.put("details", event.details());
        return body;
    }

    static Map<String, Object> searchBody(String customerId, String query, int limit) {
        Map<String, Object> match = new HashMap<>();
        match.put("match", Map.of("description", query));
        Map<String, Object> bool = new HashMap<>();
        bool.put("must", List.of(
                Map.of("term", Map.of("customerId", customerId)),
                match));
        return Map.of(
                "size", limit,
                "query", Map.of("bool", bool),
                "sort", List.of(Map.of("occurredAt", Map.of("order", "desc"))));
    }

    static List<RetrievedEvent> parseHits(JsonNode root) {
        List<RetrievedEvent> results = new ArrayList<>();
        JsonNode hits = root.path("hits").path("hits");
        for (JsonNode hit : hits) {
            String id = hit.path("_id").asText();
            float score = (float) hit.path("_score").asDouble(0);
            JsonNode source = hit.path("_source");
            EventType type = EventType.fromStreamType(source.path("eventType").asText()).orElse(null);
            if (type == null) {
                continue;
            }
            results.add(new RetrievedEvent(id, type, source.path("description").asText(), score));
        }
        return results;
    }

    private Void doIndex(TransactionEvent event) {
        client.put().uri("/{index}/_doc/{id}", indexName, documentId(event))
                .contentType(MediaType.APPLICATION_JSON)
                .body(documentBody(event))
                .retrieve().toBodilessEntity();
        return null;
    }

    private List<RetrievedEvent> doSearch(String customerId, String query, int limit) {
        JsonNode root = client.post().uri("/{index}/_search", indexName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(searchBody(customerId, query, limit))
                .retrieve()
                .body(JsonNode.class);
        return parseHits(root);
    }

    private static Map<String, Object> indexMapping() {
        return Map.of(
                "settings", Map.of("number_of_shards", 1, "number_of_replicas", 0),
                "mappings", Map.of("properties", Map.of(
                        "eventId", Map.of("type", "keyword"),
                        "eventType", Map.of("type", "keyword"),
                        "customerId", Map.of("type", "keyword"),
                        "accountId", Map.of("type", "keyword"),
                        "occurredAt", Map.of("type", "date"),
                        "description", Map.of("type", "text"),
                        "details", Map.of("type", "object", "enabled", false))));
    }
}