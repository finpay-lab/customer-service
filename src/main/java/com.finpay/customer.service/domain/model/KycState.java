package com.finpay.customer.service.domain.model;

/**
 * KYC verification state (SERVICE_CATALOG). A rejected submission may be
 * re-started; APPROVED is reached only from PENDING.
 */
public enum KycState {
    NOT_STARTED,
    PENDING,
    APPROVED,
    REJECTED;

    public boolean canTransitionTo(KycState target) {
        return switch (target) {
            case PENDING -> this == NOT_STARTED || this == REJECTED;
            case APPROVED -> this == PENDING;
            case REJECTED -> this == PENDING;
            case NOT_STARTED -> false;
        };
    }
}
