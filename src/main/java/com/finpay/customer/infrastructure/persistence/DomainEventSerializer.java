package com.finpay.customer.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.finpay.customer.domain.event.CustomerCreated;
import com.finpay.customer.domain.event.CustomerKycChanged;
import com.finpay.customer.domain.event.DomainEvent;
import org.springframework.stereotype.Component;

/**
 * Serializes domain events to the v1 event envelope
 * {@code {eventId, eventType, occurredAt, version, partitionKey, payload}}.
 * The {@code kycState} field follows contracts/events/v1/CustomerCreated.json:
 * a verified flag maps to APPROVED, otherwise NOT_STARTED.
 */
@Component
public class DomainEventSerializer {

    private final ObjectMapper objectMapper;

    public DomainEventSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(DomainEvent event) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("eventId", event.eventId().toString());
        root.put("eventType", event.eventType());
        root.put("occurredAt", event.occurredAt().toString());
        root.put("version", 1);
        root.put("partitionKey", event.partitionKey());

        ObjectNode payload = root.putObject("payload");
        if (event instanceof CustomerCreated e) {
            payload.put("customerId", e.customerId().toString());
            payload.put("status", e.status().name());
            payload.put("kycState", e.kycVerified() ? "APPROVED" : "NOT_STARTED");
            payload.put("ts", e.occurredAt().toString());
        } else if (event instanceof CustomerKycChanged e) {
            payload.put("customerId", e.customerId().toString());
            payload.put("kycVerified", e.kycVerified());
            payload.put("status", e.status().name());
            payload.put("ts", e.occurredAt().toString());
        } else {
            throw new IllegalArgumentException("Unsupported domain event: " + event.eventType());
        }

        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize outbox payload", ex);
        }
    }
}
