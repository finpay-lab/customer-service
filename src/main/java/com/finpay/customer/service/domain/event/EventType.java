package com.finpay.customer.service.domain.event;

import java.util.Optional;

/**
 * The transaction event types the explainer indexes (contracts in
 * finpay-platform/contracts/events/v1). {@code TRANSFER_REVERSED} is part of
 * the transfer saga lifecycle (see EVENT_CATALOG) even though its JSON schema
 * is not yet published in the platform contracts; the consumer parses it with
 * the same transfer payload shape.
 */
public enum EventType {

    LEDGER_ENTRY_POSTED("LedgerEntryPosted"),
    TRANSFER_CREATED("TransferCreated"),
    TRANSFER_REVERSED("TransferReversed"),
    TRANSFER_COMPLETED("TransferCompleted");

    private final String streamType;

    EventType(String streamType) {
        this.streamType = streamType;
    }

    /** The eventType discriminator used in the Kafka envelope / OpenSearch doc. */
    public String streamType() {
        return streamType;
    }

    /** Case-sensitive reverse lookup; empty when the event type is out of scope. */
    public static Optional<EventType> fromStreamType(String streamType) {
        for (EventType type : values()) {
            if (type.streamType.equals(streamType)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}