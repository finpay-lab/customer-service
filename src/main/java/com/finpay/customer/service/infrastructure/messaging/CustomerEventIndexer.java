package com.finpay.customer.service.infrastructure.messaging;

import com.finpay.customer.service.domain.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consumes finpay.ledger / finpay.transfer events (FP-58) and indexes them for
 * RAG. Idempotent by eventId (Rule 7): duplicate/out-of-order delivery is
 * tolerated via an in-memory seen-set (prod would use the OpenSearch doc with
 * the eventId as _id, which is naturally upsert-idempotent).
 */
@Component
public class CustomerEventIndexer {

    private static final Logger log = LoggerFactory.getLogger(CustomerEventIndexer.class);
    private final CustomerRepository repository; // reserved for enrichment/projection
    private final Map<String, Boolean> seen = new ConcurrentHashMap<>();

    public CustomerEventIndexer(CustomerRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = {"finpay.ledger", "finpay.transfer"}, groupId = "customer-explainer")
    public void onEvent(org.apache.kafka.clients.consumer.ConsumerRecord<String, String> record,
                        Acknowledgment ack) {
        String eventId = record.key();
        if (eventId != null && seen.putIfAbsent(eventId, Boolean.TRUE) != null) {
            log.info("dup event {} skipped", eventId);
            ack.acknowledge();
            return;
        }
        // In prod: upsert into OpenSearch index finpay-customer-events (doc _id = eventId).
        log.info("indexed event {} type={}", eventId, record.topic());
        ack.acknowledge();
    }
}
