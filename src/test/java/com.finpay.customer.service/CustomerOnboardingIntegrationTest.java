package com.finpay.customer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.customer.service.infrastructure.persistence.OutboxEventJpaEntity;
import com.finpay.customer.service.infrastructure.persistence.OutboxSpringRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @org.springframework.test.context.DynamicPropertySource
    static void datasourceProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private OutboxSpringRepository outboxRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE = "/api/v1/customers";

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

        ResponseEntity<String> activated = rest.exchange(BASE + "/" + id + "/status", HttpMethod.POST,
                json(Map.of("action", "ACTIVATE")), String.class);
        assertThat(activated.getStatusCode().value()).isEqualTo(200);
        assertThat(objectMapper.readTree(activated.getBody()).get("status").asText()).isEqualTo("ACTIVE");

        ResponseEntity<String> kyc = rest.exchange(BASE + "/" + id + "/kyc", HttpMethod.POST,
                json(Map.of("action", "START_KYC")), String.class);
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

        ResponseEntity<String> response = rest.exchange(BASE + "/" + id + "/status", HttpMethod.POST,
                json(Map.of("action", "SUSPEND")), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("code").asText()).isEqualTo("INVALID_STATE_TRANSITION");
    }

    @Test
    void missing_idempotency_key_is_bad_request() {
        ResponseEntity<String> response = rest.postForEntity(BASE + "/onboard",
                json(Map.of("email", "x@example.com", "fullName", "X")), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    private ResponseEntity<String> onboard(String key, String email, String fullName) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", key);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(Map.of("email", email, "fullName", fullName), headers);
        return rest.postForEntity(BASE + "/onboard", entity, String.class);
    }

    private HttpEntity<Map<String, Object>> json(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private ResponseEntity<String> get(UUID customerId) {
        return rest.getForEntity(BASE + "/" + customerId, String.class);
    }

    private UUID customerId(ResponseEntity<String> response) throws Exception {
        return UUID.fromString(objectMapper.readTree(response.getBody()).get("customerId").asText());
    }
}