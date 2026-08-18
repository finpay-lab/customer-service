package com.finpay.customer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.customer.service.infrastructure.persistence.OutboxEventJpaEntity;
import com.finpay.customer.service.infrastructure.persistence.OutboxSpringRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end onboarding + state-machine flow against a real PostgreSQL
 * (Testcontainers). Kafka is not started; the outbox relay is disabled so the
 * assertions target what is written transactionally, which is exactly what
 * rule 5 requires to survive without a broker.
 */
@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.outbox.relay.enabled=false",
                "spring.kafka.bootstrap-servers=localhost:9092"
        })
class CustomerOnboardingIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @LocalServerPort
    private int port;

    private final RestClient restClient = RestClient.create();

    @Autowired
    private OutboxSpringRepository outboxRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String baseUrl() {
        return "http://localhost:" + port + "/api/v1/customers";
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void onboard_creates_customer_and_outbox_row() throws Exception {
        ResponseEntity<String> response = onboard("int-key-1", "ada@example.com", "Ada Lovelace");

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        UUID customerId = customerId(response);
        assertThat(get(customerId).getStatusCode().value()).isEqualTo(200);

        List<OutboxEventJpaEntity> pending = outboxRepository
                .findByPublishedAtIsNullOrderByCreatedAtAsc(PageRequest.of(0, 100));
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getEventType()).isEqualTo("CustomerCreated");
    }

    @Test
    void replay_with_same_key_returns_original_customer() throws Exception {
        onboard("int-key-2", "grace@example.com", "Grace Hopper");
        ResponseEntity<String> replay = onboard("int-key-2", "grace@example.com", "Grace Hopper");

        assertThat(replay.getStatusCode().value()).isEqualTo(200);
        assertThat(customerId(replay)).isEqualTo(customerId(onboard("int-key-2", "grace@example.com", "Grace Hopper")));
        assertThat(outboxRepository.count()).isEqualTo(1L);
    }

    @Test
    void same_key_with_different_payload_conflicts() throws Exception {
        onboard("int-key-3", "al@example.com", "Al");
        ResponseEntity<String> conflict = onboard("int-key-3", "al@example.com", "Alice");

        assertThat(conflict.getStatusCode().value()).isEqualTo(409);
        JsonNode body = objectMapper.readTree(conflict.getBody());
        assertThat(body.get("code").asText()).isEqualTo("IDEMPOTENCY_CONFLICT");
    }

    @Test
    void duplicate_email_under_different_key_conflicts() throws Exception {
        onboard("int-key-4", "same@example.com", "Same");
        ResponseEntity<String> conflict = onboard("int-key-5", "same@example.com", "Same");

        assertThat(conflict.getStatusCode().value()).isEqualTo(409);
        JsonNode body = objectMapper.readTree(conflict.getBody());
        assertThat(body.get("code").asText()).isEqualTo("CUSTOMER_ALREADY_EXISTS");
    }

    @Test
    void status_and_kyc_transitions_append_events() throws Exception {
        onboard("int-key-6", "claude@example.com", "Claude");
        UUID id = customerId(onboard("int-key-6", "claude@example.com", "Claude"));

        ResponseEntity<String> activated = exchange(HttpMethod.POST, "/" + id + "/status",
                Map.of("action", "ACTIVATE"));
        assertThat(activated.getStatusCode().value()).isEqualTo(200);
        assertThat(objectMapper.readTree(activated.getBody()).get("status").asText()).isEqualTo("ACTIVE");

        ResponseEntity<String> kyc = exchange(HttpMethod.POST, "/" + id + "/kyc",
                Map.of("action", "START_KYC"));
        assertThat(kyc.getStatusCode().value()).isEqualTo(200);
        assertThat(objectMapper.readTree(kyc.getBody()).get("kycState").asText()).isEqualTo("PENDING");

        List<OutboxEventJpaEntity> pending = outboxRepository
                .findByPublishedAtIsNullOrderByCreatedAtAsc(PageRequest.of(0, 100));
        assertThat(pending).extracting(OutboxEventJpaEntity::getEventType)
                .containsExactlyInAnyOrder("CustomerCreated", "CustomerStatusChanged", "KycStateChanged");
    }

    @Test
    void illegal_status_transition_is_rejected() throws Exception {
        onboard("int-key-7", "alan@example.com", "Alan");
        UUID id = customerId(onboard("int-key-7", "alan@example.com", "Alan"));

        ResponseEntity<String> response = exchange(HttpMethod.POST, "/" + id + "/status",
                Map.of("action", "SUSPEND"));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("code").asText()).isEqualTo("INVALID_STATE_TRANSITION");
    }

    @Test
    void missing_idempotency_key_is_bad_request() {
        ResponseEntity<String> response = restClient.post()
                .uri(baseUrl() + "/onboard")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", "x@example.com", "fullName", "X"))
                .retrieve()
                .toEntity(String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    private ResponseEntity<String> onboard(String key, String email, String fullName) {
        return restClient.post()
                .uri(baseUrl() + "/onboard")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", email, "fullName", fullName))
                .retrieve()
                .toEntity(String.class);
    }

    private ResponseEntity<String> exchange(HttpMethod method, String path, Map<String, Object> body) {
        return restClient.method(method)
                .uri(baseUrl() + path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class);
    }

    private ResponseEntity<String> get(UUID customerId) {
        return restClient.get()
                .uri(baseUrl() + "/" + customerId)
                .retrieve()
                .toEntity(String.class);
    }

    private UUID customerId(ResponseEntity<String> response) throws Exception {
        return UUID.fromString(objectMapper.readTree(response.getBody()).get("customerId").asText());
    }
}
