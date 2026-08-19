package com.finpay.customer.service.infrastructure.opensearch;

import com.finpay.customer.service.domain.event.EventType;
import com.finpay.customer.service.domain.event.RetrievedEvent;
import com.finpay.customer.service.domain.event.TransactionEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpenSearchTransactionEventStoreTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void documentIdEqualsEventIdForIdempotentUpsert() {
        TransactionEvent event = sampleEvent();

        assertThat(OpenSearchTransactionEventStore.documentId(event)).isEqualTo(event.eventId());
    }

    @Test
    void documentBodyCarriesCustomerTypeAndTextDescription() {
        Map<String, Object> body = OpenSearchTransactionEventStore.documentBody(sampleEvent());

        assertThat(body).containsEntry("eventId", "evt-1");
        assertThat(body).containsEntry("eventType", "TransferReversed");
        assertThat(body).containsEntry("customerId", "customer-42");
        assertThat(body).containsEntry("accountId", "acct-1");
        assertThat(body.get("description")).asString().contains("reversed");
        assertThat(body.get("details")).isInstanceOf(Map.class);
    }

    @Test
    void searchBodyFiltersByCustomerAndMatchesQueryText() {
        Map<String, Object> body = OpenSearchTransactionEventStore.searchBody("customer-42", "why reversed", 5);

        assertThat(body).containsEntry("size", 5);
        List<Map<String, Object>> must = must(body);
        assertThat(term(must, 0).get("customerId")).isEqualTo("customer-42");
        assertThat(match(must, 1).get("description")).isEqualTo("why reversed");
    }

    @Test
    void parseHitsSkipsOutOfScopeTypesAndKeepsScore() throws Exception {
        JsonNode root = MAPPER.readTree("""
                {"hits":{"hits":[
                  {"_id":"evt-1","_score":0.92,"_source":{"eventType":"TransferReversed","description":"reversed"}},
                  {"_id":"evt-2","_score":0.5,"_source":{"eventType":"SomethingElse","description":"ignored"}}
                ]}}""");

        List<RetrievedEvent> hits = OpenSearchTransactionEventStore.parseHits(root);

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).eventId()).isEqualTo("evt-1");
        assertThat(hits.get(0).eventType()).isEqualTo(EventType.TRANSFER_REVERSED);
        assertThat(hits.get(0).score()).isEqualTo(0.92f);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> must(Map<String, Object> body) {
        Map<String, Object> bool = (Map<String, Object>) ((Map<String, Object>) body.get("query")).get("bool");
        return (List<Map<String, Object>>) bool.get("must");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> term(List<Map<String, Object>> must, int index) {
        return (Map<String, Object>) must.get(index).get("term");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> match(List<Map<String, Object>> must, int index) {
        return (Map<String, Object>) must.get(index).get("match");
    }

    private static TransactionEvent sampleEvent() {
        return new TransactionEvent(
                "evt-1",
                EventType.TRANSFER_REVERSED,
                "customer-42",
                "acct-1",
                Instant.parse("2026-08-12T06:34:22Z"),
                "Transfer 7 was reversed: 150.00 EUR.",
                Map.of("transferId", "7", "amount", "150.00", "currency", "EUR"));
    }
}