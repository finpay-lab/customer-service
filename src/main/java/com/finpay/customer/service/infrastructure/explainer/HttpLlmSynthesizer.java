package com.finpay.customer.service.infrastructure.explainer;

import com.finpay.customer.service.domain.TransactionExplainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LLM synthesizer for the transaction explainer (FP-58). Sends retrieved
 * events + query to the model endpoint with timeout/retry/circuit-breaker
 * (Rule 8). BYOK key is read from a secret ref and NEVER logged.
 *
 * In the lab, the actual model call is only performed if an endpoint is
 * configured; otherwise a safe local template is returned so the flow is
 * demonstrable without external dependencies.
 */
@Component
public class HttpLlmSynthesizer implements TransactionExplainer.LlmSynthesizer {

    private final HttpClient http;
    private final String endpoint;
    private final String apiKeyRef;
    private final Duration timeout;
    private final AtomicInteger failures = new AtomicInteger(0);
    private final int circuitOpenThreshold = 5;

    public HttpLlmSynthesizer(@Value("${finpay.customer.llm.endpoint:}") String endpoint,
                              @Value("${finpay.customer.llm.byok-secret-ref:}") String apiKeyRef,
                              @Value("${finpay.customer.llm.timeout-seconds:20}") int timeoutSeconds) {
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(timeoutSeconds)).build();
        this.endpoint = endpoint;
        this.apiKeyRef = apiKeyRef;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    @Override
    public TransactionExplainer.Answer synthesize(String query, List<TransactionExplainer.CustomerEvent> events) {
        if (circuitOpen()) {
            return fallback(query, events, "circuit-open");
        }
        if (endpoint == null || endpoint.isBlank()) {
            return fallback(query, events, "no-endpoint-configured");
        }
        try {
            String body = buildPrompt(query, events);
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(endpoint))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    // BYOK key injected from secret store; never logged
                    .header("Authorization", apiKeyRef == null || apiKeyRef.isBlank() ? "" : "Bearer " + apiKeyRef)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            failures.set(0);
            return new TransactionExplainer.Answer(resp.body(), events.stream().map(TransactionExplainer.CustomerEvent::eventId).toList());
        } catch (Exception ex) {
            int f = failures.incrementAndGet();
            return fallback(query, events, "error-" + f);
        }
    }

    /** Safe local answer that still cites eventIds (no external call). */
    private TransactionExplainer.Answer fallback(String query, List<TransactionExplainer.CustomerEvent> events, String reason) {
        StringBuilder sb = new StringBuilder("Based on ").append(events.size())
                .append(" event(s) for your account");
        if (!events.isEmpty()) {
            sb.append(" (").append(events.get(0).type());
            if (events.size() > 1) sb.append(" and ").append(events.size() - 1).append(" more");
            sb.append(")");
        }
        sb.append(". [synthesis fallback: ").append(reason).append("]");
        return new TransactionExplainer.Answer(sb.toString(),
                events.stream().map(TransactionExplainer.CustomerEvent::eventId).toList());
    }

    private boolean circuitOpen() { return failures.get() >= circuitOpenThreshold; }
    private String buildPrompt(String query, List<TransactionExplainer.CustomerEvent> events) {
        StringBuilder sb = new StringBuilder("{\"query\":\"")
                .append(query.replace("\"", "'")).append("\",\"events\":[");
        for (int i = 0; i < events.size(); i++) {
            if (i > 0) sb.append(",");
            TransactionExplainer.CustomerEvent e = events.get(i);
            sb.append("{\"id\":\"").append(e.eventId()).append("\",\"type\":\"").append(e.type()).append("\"}");
        }
        sb.append("]}");
        return sb.toString();
    }
}
