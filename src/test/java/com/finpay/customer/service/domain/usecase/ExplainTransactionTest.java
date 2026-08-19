package com.finpay.customer.service.domain.usecase;

import com.finpay.customer.service.domain.event.Explanation;
import com.finpay.customer.service.domain.event.RetrievedEvent;
import com.finpay.customer.service.domain.exception.ExplainUnavailableException;
import com.finpay.customer.service.domain.exception.LlmUnavailableException;
import com.finpay.customer.service.domain.port.LlmGateway;
import com.finpay.customer.service.domain.port.TransactionEventStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.finpay.customer.service.domain.event.EventType.TRANSFER_CREATED;
import static com.finpay.customer.service.domain.event.EventType.TRANSFER_REVERSED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ExplainTransactionTest {

    private final TransactionEventStore store = mock(TransactionEventStore.class);
    private final LlmGateway llm = mock(LlmGateway.class);
    private final ExplainTransaction useCase = new ExplainTransaction(store, llm, 5);

    @Test
    void synthesizesAnswerFromEvidenceAndCitesEventIds() {
        List<RetrievedEvent> evidence = List.of(
                new RetrievedEvent("evt-1", TRANSFER_REVERSED, "Transfer 42 was reversed: 150.00 EUR.", 0.9f),
                new RetrievedEvent("evt-2", TRANSFER_CREATED, "Transfer 42 was initiated: 150.00 EUR.", 0.8f));
        when(store.search("customer-42", "why was my transfer reversed?", 5)).thenReturn(evidence);
        when(llm.synthesize(eq("why was my transfer reversed?"), anyList()))
                .thenReturn("Your transfer was reversed because the risk check failed. [evt-1] [evt-2]");

        Explanation result = useCase.explain("customer-42", "why was my transfer reversed?");

        assertThat(result.answer()).contains("risk check").contains("[evt-1]");
        assertThat(result.citedEventIds()).containsExactly("evt-1", "evt-2");
        assertThat(result.evidenceFound()).isTrue();
    }

    @Test
    void returnsNoEvidenceAnswerWithoutCallingTheLlm() {
        when(store.search("customer-42", "anything", 5)).thenReturn(List.of());

        Explanation result = useCase.explain("customer-42", "anything");

        assertThat(result.evidenceFound()).isFalse();
        assertThat(result.citedEventIds()).isEmpty();
        assertThat(result.answer()).contains("No transaction events");
        verifyNoInteractions(llm);
    }

    @Test
    void fallsBackToDeterministicSummaryWhenLlmIsUnavailable() {
        List<RetrievedEvent> evidence =
                List.of(new RetrievedEvent("evt-9", TRANSFER_REVERSED, "Transfer 99 was reversed.", 1.0f));
        when(store.search("customer-42", "why", 5)).thenReturn(evidence);
        when(llm.synthesize(any(), anyList()))
                .thenThrow(new LlmUnavailableException("provider down", new RuntimeException("boom")));

        Explanation result = useCase.explain("customer-42", "why");

        assertThat(result.answer()).contains("currently unavailable").contains("evt-9");
        assertThat(result.citedEventIds()).containsExactly("evt-9");
        assertThat(result.evidenceFound()).isTrue();
    }

    @Test
    void propagatesSearchUnavailability() {
        when(store.search("customer-42", "why", 5))
                .thenThrow(new ExplainUnavailableException("index down", new RuntimeException("boom")));

        assertThatThrownBy(() -> useCase.explain("customer-42", "why"))
                .isInstanceOf(ExplainUnavailableException.class);
        verifyNoInteractions(llm);
    }
}