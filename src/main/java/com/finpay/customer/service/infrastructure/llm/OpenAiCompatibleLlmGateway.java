package com.finpay.customer.service.infrastructure.llm;

import com.finpay.customer.service.domain.event.RetrievedEvent;
import com.finpay.customer.service.domain.exception.LlmUnavailableException;
import com.finpay.customer.service.domain.port.LlmGateway;
import com.finpay.customer.service.infrastructure.resilience.CircuitBreaker;
import com.finpay.customer.service.infrastructure.resilience.Retry;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OpenAI-compatible chat-completions gateway. Owns all Rule 8 concerns:
 * timeouts (via the injected RestClient), retry with backoff, and a circuit
 * breaker. The BYOK key is bound from the secret store ({@code FINPAY_LLM_API_KEY})
 * and is NEVER logged — logs only carry exception messages/statuses.
 */
public final class OpenAiCompatibleLlmGateway implements LlmGateway {

    private static final Logger LOG = LoggerFactory.getLogger(OpenAiCompatibleLlmGateway.class);
    private static final String SYSTEM_PROMPT = """
            You are FinPay's transaction assistant. Explain the customer's question
            about their account activity in plain, friendly language. Ground your
            answer ONLY on the provided events. Always cite the event IDs you base
            your answer on, using the form [eventId]. If the events do not answer
            the question, say so and cite what is available.""";

    private final RestClient client;
    private final String model;
    private final int maxTokens;
    private final double temperature;
    private final CircuitBreaker circuitBreaker;
    private final int maxRetries;
    private final Duration retryBackoff;
    private final String apiKey;

    public OpenAiCompatibleLlmGateway(RestClient client, String model, int maxTokens, double temperature,
                                      CircuitBreaker circuitBreaker, int maxRetries, Duration retryBackoff,
                                      String apiKey) {
        this.client = client;
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        this.circuitBreaker = circuitBreaker;
        this.maxRetries = maxRetries;
        this.retryBackoff = retryBackoff;
        this.apiKey = apiKey;
    }

    @Override
    public String synthesize(String userQuery, List<RetrievedEvent> evidence) {
        try {
            return circuitBreaker.execute(() ->
                    Retry.withBackoff(maxRetries, retryBackoff, () -> doSynthesize(userQuery, evidence)));
        } catch (RuntimeException e) {
            LOG.warn("LLM synthesis failed ({} attempt(s)): {}", maxRetries, e.getMessage());
            throw new LlmUnavailableException("LLM provider unavailable", e);
        }
    }

    private String doSynthesize(String userQuery, List<RetrievedEvent> evidence) {
        Map<String, Object> request = Map.of(
                "model", model,
                "temperature", temperature,
                "max_tokens", maxTokens,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userPrompt(userQuery, evidence))));

        JsonNode root = client.post().uri("/chat/completions")
                .headers(headers -> headers.setBearerAuth(apiKey))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(JsonNode.class);

        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.asText().isBlank()) {
            throw new IllegalStateException("LLM response contained no content");
        }
        return content.asText();
    }

    private static String userPrompt(String userQuery, List<RetrievedEvent> evidence) {
        String context = evidence.stream()
                .map(event -> "- [%s] (%s) %s".formatted(event.eventId(), event.eventType().streamType(), event.description()))
                .collect(Collectors.joining("\n"));
        return "Question: %s%n%nRelevant events:%n%s%n%nAnswer the question using only these events and cite their event IDs."
                .formatted(userQuery, context);
    }
}