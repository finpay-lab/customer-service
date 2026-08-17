package com.finpay.customer.service.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Customer lifecycle status (SERVICE_CATALOG). Every transition is validated
 * against the legal-transition table below; illegal transitions are rejected
 * with {@link com.finpay.customer.service.domain.exception.IllegalStateTransitionException}
 * (AGENTS.md rule 9). CLOSED is terminal.
 */
public enum CustomerStatus {
    PENDING,
    ACTIVE,
    SUSPENDED,
    BLOCKED,
    CLOSED;

    private static final Set<CustomerStatus> ACTIVATABLE = EnumSet.of(PENDING, SUSPENDED);
    private static final Set<CustomerStatus> SUSPENDABLE = EnumSet.of(ACTIVE);
    private static final Set<CustomerStatus> BLOCKABLE = EnumSet.of(PENDING, ACTIVE, SUSPENDED);

    /** Returns the status reached by {@code this} → {@code target}, or null if illegal. */
    public boolean canTransitionTo(CustomerStatus target) {
        return switch (target) {
            case ACTIVE -> ACTIVATABLE.contains(this);
            case SUSPENDED -> SUSPENDABLE.contains(this);
            case BLOCKED -> BLOCKABLE.contains(this);
            case CLOSED -> this != CLOSED;
            case PENDING -> false;
        };
    }
}
