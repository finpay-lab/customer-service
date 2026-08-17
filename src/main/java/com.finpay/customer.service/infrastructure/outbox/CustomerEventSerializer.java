package com.finpay.customer.service.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.customer.service.domain.event.CustomerCreatedEvent;
import com.finpay.customer.service.domain.event.CustomerStatusChangedEvent;
import com.finpay.customer.service.domain.event.DomainEvent;
import com.finpay.customer.service.domain.event.KycStateChangedEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Serializes domain events to the `contracts/events/v1/*` envelope shape
 * (eventId/eventType/occurredAt/version/partitionKey/payload). Pure
 * infrastructure concern — the domain stays free of JSON.
 */
@Component
public class CustomerEventSerializer {

    private final ObjectMapper objectMapper;

    public CustomerEventSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(toEnvelope(event));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize " + event.eventType() + " to JSON", e);
        }
    }

    private Map<String, Object> toEnvelope(DomainEvent event) {
        return switch (event) {
            case CustomerCreatedEvent e -> envelope(
                    e.eventType(), e.eventId(), e.customerId(), e.ts(),
                    Map.of(
                            "customerId", e.customerId().toString(),
                            "status", e.status().name(),
                            "kycState", e.kycState().name(),
                            "ts", e.ts().toString()));
            case CustomerStatusChangedEvent e -> envelope(
                    e.eventType(), e.eventId(), e.customerId(), e.ts(),
                    Map.of(
                            "customerId", e.customerId().toString(),
                            "previousStatus", e.previousStatus().name(),
                            "newStatus", e.newStatus().name(),
                            "ts", e.ts().toString()));
            case KycStateChangedEvent e -> envelope(
                    e.eventType(), e.eventId(), e.customerId(), e.ts(),
                    Map.of(
                            "customerId", e.customerId().toString(),
                            "previousState", e.previousState().name(),
                            "newState", e.newState().name(),
                            "ts", e.ts().toString()));
            default -> throw new IllegalArgumentException("Unknown domain event " + event);
        };
    }

    private Map<String, Object> envelope(String eventType, UUID eventId, UUID partitionKey,
                                         Instant occurredAt, Map<String, Object> payload) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", eventId.toString());
        envelope.put("eventType", eventType);
        envelope.put("occurredAt", occurredAt.toString());
        envelope.put("version", 1);
        envelope.put("partitionKey", partitionKey.toString());
        envelope.put("payload", payload);
        return envelope;
    }
}