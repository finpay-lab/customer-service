package com.finpay.customer.service.domain.event;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionEventDescriptionsTest {

    @Test
    void describesLedgerEntryPosting() {
        Map<String, Object> payload = Map.of(
                "postingId", "5d6e7f80-91a2-b3c4-d5e6-f7a819202122",
                "accountId", "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "debit", "150.00", "credit", "0.00", "amount", "150.00",
                "currency", "EUR", "ts", "2026-08-12T06:34:22.400000Z");

        String description = TransactionEventDescriptions.describe(EventType.LEDGER_ENTRY_POSTED, payload);

        assertThat(description)
                .contains("150.00").contains("EUR").contains("a1b2c3d4")
                .contains("debit 150.00").contains("credit 0.00");
    }

    @Test
    void describesTransferLifecycleSteps() {
        Map<String, Object> payload = Map.of(
                "transferId", "c3d4e5f6-0a1b-2c3d-4e5f-6a7b8c9d0e1f",
                "from", "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                "to", "11223344-5566-7788-99aa-bbccddeeff00",
                "amount", "150.00", "currency", "EUR");

        assertThat(TransactionEventDescriptions.describe(EventType.TRANSFER_CREATED, payload))
                .contains("was initiated").contains("150.00 EUR").contains("to account 11223344");
        assertThat(TransactionEventDescriptions.describe(EventType.TRANSFER_REVERSED, payload))
                .contains("was reversed");
        assertThat(TransactionEventDescriptions.describe(EventType.TRANSFER_COMPLETED, payload))
                .contains("was completed");
    }

    @Test
    void toleratesMissingPayloadFields() {
        String description = TransactionEventDescriptions.describe(EventType.TRANSFER_CREATED, Map.of());
        assertThat(description).contains("unknown");
    }
}