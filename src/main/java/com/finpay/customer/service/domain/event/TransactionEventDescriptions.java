package com.finpay.customer.service.domain.event;

import java.util.Map;

/**
 * Renders a human-readable description of a transaction event from its raw
 * payload. The description is what OpenSearch full-text matches against, so it
 * keeps the retrieval index free of domain terminology concerns. Pure function,
 * unit-tested.
 */
public final class TransactionEventDescriptions {

    private TransactionEventDescriptions() {
    }

    public static String describe(EventType type, Map<String, Object> payload) {
        return switch (type) {
            case LEDGER_ENTRY_POSTED -> ledgerEntryPosted(payload);
            case TRANSFER_CREATED -> transfer("initiated", payload);
            case TRANSFER_REVERSED -> transfer("reversed", payload);
            case TRANSFER_COMPLETED -> transfer("completed", payload);
        };
    }

    private static String ledgerEntryPosted(Map<String, Object> payload) {
        return "Ledger entry %s posted %s %s to account %s (debit %s, credit %s)."
                .formatted(str(payload, "postingId"), str(payload, "amount"),
                        str(payload, "currency"), str(payload, "accountId"),
                        str(payload, "debit"), str(payload, "credit"));
    }

    private static String transfer(String verb, Map<String, Object> payload) {
        return "Transfer %s was %s: %s %s from account %s to account %s."
                .formatted(str(payload, "transferId"), verb, str(payload, "amount"),
                        str(payload, "currency"), str(payload, "from"), str(payload, "to"));
    }

    private static String str(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "unknown" : String.valueOf(value);
    }
}