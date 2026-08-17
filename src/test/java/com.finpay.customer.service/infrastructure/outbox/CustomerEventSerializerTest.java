package com.finpay.customer.service.infrastructure.outbox;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.customer.service.domain.event.CustomerCreatedEvent;
import com.finpay.customer.service.domain.event.CustomerStatusChangedEvent;
import com.finpay.customer.service.domain.event.KycStateChangedEvent;
import com.finpay.customer.service.domain.model.Customer;
import com.finpay.customer.service.domain.model.CustomerStatus;
import com.finpay.customer.service.domain.model.KycState;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerEventSerializerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final CustomerEventSerializer serializer = new CustomerEventSerializer(objectMapper);

    @Test
    void customer_created_matches_contract_envelope() throws Exception {
        Customer customer = Customer.onboard("ada@example.com", "Ada Lovelace");
        String json = serializer.serialize(CustomerCreatedEvent.of(customer));

        Map<String, Object> envelope = objectMapper.readValue(json, new TypeReference<>() {});
        assertThat(envelope.get("eventType")).isEqualTo("CustomerCreated");
        assertThat(envelope.get("version")).isEqualTo(1);
        assertThat(envelope.get("partitionKey")).isEqualTo(customer.id().toString());
        assertThat(envelope).containsKeys("eventId", "occurredAt");

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) envelope.get("payload");
        assertThat(payload.get("customerId")).isEqualTo(customer.id().toString());
        assertThat(payload.get("status")).isEqualTo(CustomerStatus.PENDING.name());
        assertThat(payload.get("kycState")).isEqualTo(KycState.NOT_STARTED.name());
        assertThat(payload).containsKey("ts");
    }

    @Test
    void status_changed_envelope_carries_both_states() throws Exception {
        String json = serializer.serialize(new CustomerStatusChangedEvent(
                java.util.UUID.randomUUID(), java.util.UUID.randomUUID(),
                CustomerStatus.PENDING, CustomerStatus.ACTIVE, java.time.Instant.now()));

        Map<String, Object> envelope = objectMapper.readValue(json, new TypeReference<>() {});
        assertThat(envelope.get("eventType")).isEqualTo("CustomerStatusChanged");

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) envelope.get("payload");
        assertThat(payload.get("previousStatus")).isEqualTo("PENDING");
        assertThat(payload.get("newStatus")).isEqualTo("ACTIVE");
    }

    @Test
    void kyc_changed_envelope_carries_both_states() throws Exception {
        String json = serializer.serialize(new KycStateChangedEvent(
                java.util.UUID.randomUUID(), java.util.UUID.randomUUID(),
                KycState.NOT_STARTED, KycState.PENDING, java.time.Instant.now()));

        Map<String, Object> envelope = objectMapper.readValue(json, new TypeReference<>() {});
        assertThat(envelope.get("eventType")).isEqualTo("KycStateChanged");

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) envelope.get("payload");
        assertThat(payload.get("previousState")).isEqualTo("NOT_STARTED");
        assertThat(payload.get("newState")).isEqualTo("PENDING");
    }
}