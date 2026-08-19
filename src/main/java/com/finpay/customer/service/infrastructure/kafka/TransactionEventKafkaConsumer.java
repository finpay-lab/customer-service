package com.finpay.customer.service.infrastructure.kafka;

import com.finpay.customer.service.domain.event.EventType;
import com.finpay.customer.service.domain.event.TransactionEvent;
import com.finpay.customer.service.domain.event.TransactionEventDescriptions;
import com.finpay.customer.service.domain.port.TransactionEventStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

/**
 * Indexes transaction events into the RAG read-model. Consumes the shared
 * {@code finpay.ledger} and {@code finpay.transfer} topics (see EVENT_CATALOG),
 * keyed by customerId.
 *
 * <p>Rule 7 (duplicates / out-of-order): each record is an independent
 * idempotent upsert keyed by {@code eventId} (the OpenSearch document id), so
 * redelivery and reordering cannot corrupt the index.
 *
 * <p>Poison messages are logged and skipped rather than stalling the consumer
 * group (default error handlers would retry forever); the read model is
 * best-effort by design (ADR-0010).
 */
@Component
public class TransactionEventKafkaConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionEventKafkaConsumer.class);

    private final ObjectMapper objectMapper;
    private final TransactionEventStore eventStore;

    public TransactionEventKafkaConsumer(ObjectMapper objectMapper, TransactionEventStore eventStore) {
        this.objectMapper = objectMapper;
        this.eventStore = eventStore;
    }

    @KafkaListener(
            topics = "#{'${finpay.explain.kafka.topics}'.split(',')}",
            groupId = "${finpay.explain.kafka.group-id}")
    public void onEvent(ConsumerRecord<String, String> record) {
        String customerId = record.key();
        if (customerId == null || customerId.isBlank()) {
            LOG.warn("Dropping {} event without a customerId key (offset {} in partition {})",
                    record.topic(), record.offset(), record.partition());
            return;
        }
        try {
            EventEnvelope envelope = objectMapper.readValue(record.value(), EventEnvelope.class);
            EventType type = EventType.fromStreamType(envelope.eventType()).orElse(null);
            if (type == null) {
                LOG.debug("Ignoring out-of-scope event type {} on topic {}", envelope.eventType(), record.topic());
                return;
            }
            eventStore.index(toDomain(envelope, type, customerId));
        } catch (IOException e) {
            LOG.warn("Unparseable event payload on {} (offset {}): {}", record.topic(), record.offset(), e.getMessage());
        }
    }

    static TransactionEvent toDomain(EventEnvelope envelope, EventType type, String customerId) {
        String accountId = primaryAccountId(type, envelope.payload());
        return new TransactionEvent(
                envelope.eventId(),
                type,
                customerId,
                accountId,
                envelope.occurredAt(),
                TransactionEventDescriptions.describe(type, envelope.payload()),
                envelope.payload());
    }

    private static String primaryAccountId(EventType type, Map<String, Object> payload) {
        String key = type == EventType.LEDGER_ENTRY_POSTED ? "accountId" : "from";
        Object value = payload.get(key);
        return value == null ? null : String.valueOf(value);
    }

    /** Shape of the shared event envelope (finpay-platform/contracts/events/v1). */
    record EventEnvelope(String eventId, String eventType, Instant occurredAt,
                         Integer version, String partitionKey, Map<String, Object> payload) {
    }
}