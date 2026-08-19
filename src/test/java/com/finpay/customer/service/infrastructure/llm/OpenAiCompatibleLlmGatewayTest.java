package com.finpay.customer.service.infrastructure.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.customer.service.domain.event.RetrievedEvent;
import com.finpay.customer.service.domain.exception.LlmUnavailableException;
import com.finpay.customer.service.infrastructure.resilience.CircuitBreaker;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import static com.finpay.customer.service.domain.event.EventType.TRANSFER_REVERSED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenAiCompatibleLlmGatewayTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final List<RetrievedEvent> EVIDENCE =
            List.of(new RetrievedEvent("evt-1", TRANSFER_REVERSED, "Transfer 42 was reversed: 150.00 EUR.", 0.9f));

    @Test
    void sendsBearerKeyAndReturnsSynthesizedContent() throws Exception {
        JsonNode response = MAPPER.readTree(
                "{\"choices\":[{\"message\":{\"content\":\"Your transfer was reversed. [evt-1]\"}}]}");
        RestClient client = mock(RestClient.class, RETURNS_DEEP_STUBS);
        when(client.post().uri("/chat/completions").headers(any()).contentType(any())
                .body(any()).retrieve().body(JsonNode.class)).thenReturn(response);

        OpenAiCompatibleLlmGateway gateway = gateway(client, 1);
        String answer = gateway.synthesize("why was my transfer reversed?", EVIDENCE);

        assertThat(answer).isEqualTo("Your transfer was reversed. [evt-1]");

        Consumer<HttpHeaders> headersConsumer = capturedHeaders(client);
        HttpHeaders headers = new HttpHeaders();
        headersConsumer.accept(headers);
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer sk-test-secret");
    }

    @Test
    void failsAsLlmUnavailableWhenResponseHasNoContent() throws Exception {
        JsonNode empty = MAPPER.readTree("{\"choices\":[]}");
        RestClient client = mock(RestClient.class, RETURNS_DEEP_STUBS);
        when(client.post().uri("/chat/completions").headers(any()).contentType(any())
                .body(any()).retrieve().body(JsonNode.class)).thenReturn(empty);

        OpenAiCompatibleLlmGateway gateway = gateway(client, 1);

        assertThatThrownBy(() -> gateway.synthesize("why?", EVIDENCE))
                .isInstanceOf(LlmUnavailableException.class);
    }

    @SuppressWarnings("unchecked")
    private static Consumer<HttpHeaders> capturedHeaders(RestClient client) {
        var captor = org.mockito.ArgumentCaptor.forClass(Consumer.class);
        verify(client.post()).headers(captor.capture());
        return captor.getValue();
    }

    private static OpenAiCompatibleLlmGateway gateway(RestClient client, int maxRetries) {
        return new OpenAiCompatibleLlmGateway(
                client, "finpay-model", 512, 0.2,
                new CircuitBreaker(3, Duration.ofSeconds(60), 2),
                maxRetries, Duration.ZERO, "sk-test-secret");
    }
}