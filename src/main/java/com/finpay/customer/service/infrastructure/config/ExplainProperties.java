package com.finpay.customer.service.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Explainer configuration (application.yml). The LLM api-key is injected from
 * the secret store ({@code FINPAY_LLM_API_KEY}) and must never appear in logs.
 */
@ConfigurationProperties(prefix = "finpay.explain")
public record ExplainProperties(
        String indexName,
        int maxEvidence,
        Kafka kafka,
        Opensearch opensearch,
        Llm llm) {

    public record Kafka(String topics, String groupId) {
    }

    public record Opensearch(
            String baseUrl,
            Duration connectTimeout,
            Duration readTimeout,
            int maxRetries,
            Duration retryBackoff,
            CircuitBreakerSettings circuitBreaker) {
    }

    public record Llm(
            String baseUrl,
            String model,
            int maxTokens,
            double temperature,
            Duration connectTimeout,
            Duration readTimeout,
            int maxRetries,
            Duration retryBackoff,
            CircuitBreakerSettings circuitBreaker,
            String apiKey) {
    }

    public record CircuitBreakerSettings(int failureThreshold, Duration resetTimeout, int halfOpenMaxCalls) {
    }
}