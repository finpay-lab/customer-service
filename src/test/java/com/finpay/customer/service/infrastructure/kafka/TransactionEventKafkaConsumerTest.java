package com.finpay.customer.service.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import com.finpay.customer.service.domain.event.EventType;
import com.finpay.customer.service.domain.event.TransactionEvent;
import com.finpay.customer.service.domain.port.TransactionEventStore;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class TransactionEventKafkaConsumerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void indexesLedgerEntryPostedWithCustomerFromRecordKey() {
        TransactionEventStore store = mock(TransactionEventStore.class);
        TransactionEventKafkaConsumer consumer = new TransactionEventKafkaConsumer(MAPPER, store);
        String value = """
                {"eventId":"4c5d6e7f-8091-a2b3-c4d5-e6f7a8192021","eventType":"LedgerEntryPosted",
                 "occurredAt":"2026-08-12T06:34:22.500000Z","version":1,
                 "partitionKey":"a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                 "payload":{"postingId":"5d6e7f80-91a2-b3c4-d5e6-f7a819202122",
                 "accountId":"a1b2c3d4-e5f6-7890-abcd-ef1234567890","debit":"150.00","credit":"0.00",
                 "amount":"150.00","currency":"EUR","ts":"2026-08-12T06:34:22.400000Z"}}""";

        consumer.onEvent(new ConsumerRecord<>("finpay.ledger", 0, 1L, "customer-42", value));

        ArgumentCaptor<TransactionEvent> captor = ArgumentCaptor.forClass(TransactionEvent.class);
        verify(store).index(captor.capture());
        TransactionEvent event = captor.getValue();
        assertThat(event.eventId()).isEqualTo("4c5d6e7f-8091-a2b3-c4d5-e6f7a8192021");
        assertThat(event.customerId()).isEqualTo("customer-42");
        assertThat(event.eventType()).isEqualTo(EventType.LEDGER_ENTRY_POSTED);
        assertThat(event.accountId()).isEqualTo("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
        assertThat(event.description()).contains("150.00").contains("EUR");
    }

    @Test
    void indexesTransferCompletedUsingFromAccountAsPrimary() {
        TransactionEventStore store = mock(TransactionEventStore.class);
        TransactionEventKafkaConsumer consumer = new TransactionEventKafkaConsumer(MAPPER, store);
        String value = """
                {"eventId":"2a3b4c5d-6e7f-8091-a2b3-c4d5e6f70819","eventType":"TransferCompleted",
                 "occurredAt":"2026-08-12T06:34:22.000000Z","version":1,
                 "partitionKey":"c3d4e5f6-0a1b-2c3d-4e5f-6a7b8c9d0e1f",
                 "payload":{"transferId":"c3d4e5f6-0a1b-2c3d-4e5f-6a7b8c9d0e1f",
                 "from":"a1b2c3d4-e5f6-7890-abcd-ef1234567890","to":"11223344-5566-7788-99aa-bbccddeeff00",
                 "amount":"150.00","currency":"EUR","status":"COMPLETED","sagaStep":"FINALIZATION"}}""";

        consumer.onEvent(new ConsumerRecord<>("finpay.transfer", 2, 7L, "customer-42", value));

        ArgumentCaptor<TransactionEvent> captor = ArgumentCaptor.forClass(TransactionEvent.class);
        verify(store).index(captor.capture());
        TransactionEvent event = captor.getValue();
        assertThat(event.eventType()).isEqualTo(EventType.TRANSFER_COMPLETED);
        assertThat(event.accountId()).isEqualTo("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
        assertThat(event.description()).contains("was completed").contains("150.00 EUR");
    }

    @Test
    void skipsOutOfScopeEventTypes() {
        TransactionEventStore store = mock(TransactionEventStore.class);
        TransactionEventKafkaConsumer consumer = new TransactionEventKafkaConsumer(MAPPER, store);
        String value = """
                {"eventId":"9a9a9a9a-0000-0000-0000-000000000000","eventType":"PaymentCompleted",
                 "occurredAt":"2026-08-12T06:34:22.000000Z","version":1,
                 "partitionKey":"x","payload":{"paymentId":"y","amount":"10.00","currency":"EUR"}}""";

        consumer.onEvent(new ConsumerRecord<>("finpay.transfer", 0, 0L, "customer-42", value));

        verify(store, never()).index(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void dropsRecordsWithoutACustomerKey() {
        TransactionEventStore store = mock(TransactionEventStore.class);
        TransactionEventKafkaConsumer consumer = new TransactionEventKafkaConsumer(MAPPER, store);

        consumer.onEvent(new ConsumerRecord<>("finpay.ledger", 0, 0L, null, "{}"));

        verify(store, never()).index(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void redeliveryReplaysTheSameEventIdForIdempotentUpsert() {
        TransactionEventStore store = mock(TransactionEventStore.class);
        TransactionEventKafkaConsumer consumer = new TransactionEventKafkaConsumer(MAPPER, store);
        String value = """
                {"eventId":"4c5d6e7f-8091-a2b3-c4d5-e6f7a8192021","eventType":"TransferReversed",
                 "occurredAt":"2026-08-12T06:34:22.500000Z","version":1,
                 "partitionKey":"c3d4e5f6-0a1b-2c3d-4e5f-6a7b8c9d0e1f",
                 "payload":{"transferId":"c3d4e5f6-0a1b-2c3d-4e5f-6a7b8c9d0e1f",
                 "from":"a1b2c3d4-e5f6-7890-abcd-ef1234567890","to":"11223344-5566-7788-99aa-bbccddeeff00",
                 "amount":"150.00","currency":"EUR","status":"REVERSED","sagaStep":"COMPENSATION"}}""";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("finpay.transfer", 1, 42L, "customer-42", value);

        consumer.onEvent(record);
        consumer.onEvent(record); // at-least-once redelivery

        ArgumentCaptor<TransactionEvent> captor = ArgumentCaptor.forClass(TransactionEvent.class);
        verify(store, times(2)).index(captor.capture());
        assertThat(captor.getAllValues())
                .allSatisfy(e -> assertThat(e.eventId()).isEqualTo("4c5d6e7f-8091-a2b3-c4d5-e6f7a8192021"));
    }
}