package com.finpay.customer.domain.model;

/**
 * Customer lifecycle status. Transitions are legal only when declared in
 * {@link Customer}; anything else throws {@link IllegalStateTransitionException}
 * (AGENTS.md rule 9).
 *
 * <pre>
 * KYC_PENDING --(approveKyc)--> ACTIVE --(freeze)--> FROZEN
 *      |                          |   |                |
 *      |                          |   +--(unfreeze)-->+
 *      +----(close)-------------->+   +--(revokeKyc)-->KYC_PENDING
 * FROZEN --(close)--> CLOSED (terminal)
 * </pre>
 */
public enum CustomerStatus {
    /** Initial status: KYC verification not yet approved. */
    KYC_PENDING,
    /** Fully onboarded and able to transact. */
    ACTIVE,
    /** Temporarily blocked by the platform; no transactions. */
    FROZEN,
    /** Terminal status; the customer record is closed. */
    CLOSED
}
