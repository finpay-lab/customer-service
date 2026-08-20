package com.finpay.customer.service.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionExplainerTest {

    static final class FakeStore implements TransactionExplainer.EventStore {
        final List<TransactionExplainer.CustomerEvent> events;
        FakeStore(List<TransactionExplainer.CustomerEvent> e) { this.events = e; }
        @Override public List<TransactionExplainer.CustomerEvent> retrieve(String cid, String q) { return events; }
    }

    static final class FakeLlm implements TransactionExplainer.LlmSynthesizer {
        @Override public TransactionExplainer.Answer synthesize(String q, List<TransactionExplainer.CustomerEvent> e) {
            return new TransactionExplainer.Answer("Your transfer was reversed because of " + e.size() + " event(s).",
                    e.stream().map(TransactionExplainer.CustomerEvent::eventId).toList());
        }
    }

    @Test
    void explainsCitingEventIds() {
        List<TransactionExplainer.CustomerEvent> evs = List.of(
                new TransactionExplainer.CustomerEvent("evt-1", "c1", "TransferReversed", "{}", ""));
        TransactionExplainer ex = new TransactionExplainer(new FakeStore(evs), new FakeLlm());
        TransactionExplainer.Answer a = ex.explain("c1", "why was my transfer reversed?");
        assertThat(a.text()).contains("reversed");
        assertThat(a.citedEventIds()).containsExactly("evt-1");
    }
}
