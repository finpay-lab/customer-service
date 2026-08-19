package com.finpay.customer.service.infrastructure.config;

import com.finpay.customer.service.domain.port.LlmGateway;
import com.finpay.customer.service.domain.port.TransactionEventStore;
import com.finpay.customer.service.domain.usecase.ExplainTransaction;
import com.finpay.customer.service.infrastructure.llm.OpenAiCompatibleLlmGateway;
import com.finpay.customer.service.infrastructure.opensearch.OpenSearchTransactionEventStore;
import com.finpay.customer.service.infrastructure.resilience.CircuitBreaker;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Wires the explainer: two RestClient instances (OpenSearch + LLM, each with
 * its own timeouts), the OpenSearch read-model store, the LLM gateway, and the
 * domain use case. Domain classes stay free of Spring — they are constructed
 * here as plain objects (Rule 4).
 */
@Configuration
@EnableConfigurationProperties(ExplainProperties.class)
public class ExplainConfiguration {

    @Bean
    public RestClient opensearchRestClient(ExplainProperties props, RestClient.Builder builder) {
        return builder
                .baseUrl(props.opensearch().baseUrl())
                .requestFactory(clientSettings(props.opensearch().connectTimeout(), props.opensearch().readTimeout()))
                .build();
    }

    @Bean
    public RestClient llmRestClient(ExplainProperties props, RestClient.Builder builder) {
        return builder
                .baseUrl(props.llm().baseUrl())
                .requestFactory(clientSettings(props.llm().connectTimeout(), props.llm().readTimeout()))
                .build();
    }

    @Bean
    public TransactionEventStore transactionEventStore(ExplainProperties props, RestClient opensearchRestClient) {
        return new OpenSearchTransactionEventStore(
                opensearchRestClient,
                props.indexName(),
                circuitBreaker(props.opensearch().circuitBreaker()),
                props.opensearch().maxRetries(),
                props.opensearch().retryBackoff());
    }

    @Bean
    public LlmGateway llmGateway(ExplainProperties props, RestClient llmRestClient) {
        return new OpenAiCompatibleLlmGateway(
                llmRestClient,
                props.llm().model(),
                props.llm().maxTokens(),
                props.llm().temperature(),
                circuitBreaker(props.llm().circuitBreaker()),
                props.llm().maxRetries(),
                props.llm().retryBackoff(),
                props.llm().apiKey());
    }

    @Bean
    public ExplainTransaction explainTransaction(ExplainProperties props,
                                                 TransactionEventStore transactionEventStore,
                                                 LlmGateway llmGateway) {
        return new ExplainTransaction(transactionEventStore, llmGateway, props.maxEvidence());
    }

    /** Best-effort index bootstrap on startup (search is a derived, rebuildable index — ADR-0010). */
    @Bean
    public ApplicationRunner ensureOpenSearchIndex(TransactionEventStore store) {
        return args -> {
            if (store instanceof OpenSearchTransactionEventStore openSearchStore) {
                openSearchStore.createIndexIfMissing();
            }
        };
    }

    private static CircuitBreaker circuitBreaker(ExplainProperties.CircuitBreakerSettings settings) {
        return new CircuitBreaker(settings.failureThreshold(), settings.resetTimeout(), settings.halfOpenMaxCalls());
    }

    private static SimpleClientHttpRequestFactory clientSettings(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) connectTimeout.toMillis());
        factory.setReadTimeout((int) readTimeout.toMillis());
        return factory;
    }
}