package com.finpay.customer.domain.model;

import java.util.UUID;

/**
 * Thrown when an operation would move the customer to a state that is not a
 * legal transition of the status machine (AGENTS.md rule 9). Transport-agnostic;
 * the API layer maps it to HTTP 409 {@code INVALID_STATE_TRANSITION}.
 */
public final class IllegalStateTransitionException extends RuntimeException {

    public IllegalStateTransitionException(
            UUID customerId,
            String operation,
            CustomerStatus from,
            CustomerStatus[] legalFrom) {
        super("Customer %s cannot %s from %s; legal source states are %s"
                .formatted(customerId, operation, from, java.util.Arrays.toString(legalFrom)));
    }
}
