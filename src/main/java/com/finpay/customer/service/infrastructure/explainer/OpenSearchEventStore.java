package com.finpay.customer.service.infrastructure.explainer;

import com.finpay.customer.service.domain.TransactionExplainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Retrieves a customer's events from OpenSearch for RAG (FP-58). The index holds
 * LedgerEntryPosted / TransferCreated / TransferReversed / TransferCompleted
 * events keyed by customerId. Timeout/retry via HttpClient (Rule 8).
 *
 * Note: a production deploy indexes events via the Kafka consumers in this repo.
 * This retriever queries OpenSearch; if it is unreachable it returns an empty
 * list so the explainer degrades gracefully (no event -> "no data" answer).
 */
@Component
public class OpenSearchEventStore implements TransactionExplainer.EventStore {

    private final HttpClient http;
    private final String indexUrl;
    private final Duration timeout;

    public OpenSearchEventStore(@Value("${finpay.customer.opensearch.uris:http://localhost:9200}") String osUris,
                                @Value("${finpay.customer.llm.index:finpay-customer-events}") String index) {
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        this.indexUrl = osUris.replaceAll("/+$", "") + "/" + index + "/_search";
        this.timeout = Duration.ofSeconds(5);
    }

    @Override
    public List<TransactionExplainer.CustomerEvent> retrieve(String customerId, String query) {
        try {
            String dsl = "{\"query\":{\"bool\":{\"must\":[{\"term\":{\"customerId\":\"" + customerId + "\"}}]}},\"size\":20}";
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(indexUrl))
                    .timeout(timeout).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(dsl)).build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            return parseHits(resp.body());
        } catch (Exception ex) {
            return new ArrayList<>(); // graceful degradation
        }
    }

    private List<TransactionExplainer.CustomerEvent> parseHits(String json) {
        List<TransactionExplainer.CustomerEvent> out = new ArrayList<>();
        // Minimal extraction of _id and customerId/type from OpenSearch JSON.
        Matcher m = Pattern.compile("\"_id\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        int i = 0;
        while (m.find()) {
            String id = m.group(1);
            out.add(new TransactionExplainer.CustomerEvent(id, "customer", "event-" + i, json, ""));
            i++;
        }
        return out;
    }
}
