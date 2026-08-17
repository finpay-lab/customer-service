package com.finpay.customer.service.domain.exception;

/**
 * Raised when a state transition is not in the aggregate's legal-transition
 * table (AGENTS.md rule 9). Domain exception — infrastructure-free.
 */
public class IllegalStateTransitionException extends RuntimeException {

    public IllegalStateTransitionException(String message) {
        super(message);
    }
}
