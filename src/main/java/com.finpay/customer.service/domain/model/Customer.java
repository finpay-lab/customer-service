package com.finpay.customer.service.domain.model;

import com.finpay.customer.service.domain.exception.IllegalStateTransitionException;

import java.time.Instant;
import java.util.UUID;

/**
 * Customer profile aggregate — the bounded context's root. Encapsulates the
 * profile data plus the lifecycle status (PENDING/ACTIVE/SUSPENDED/BLOCKED/
 * CLOSED) and KYC state machines. Every transition validates the legal table
 * and rejects illegal moves (AGENTS.md rule 9); the row is versioned so two
 * concurrent transitions cannot silently overwrite each other.
 *
 * <p>Pure Java / no framework imports (AGENTS.md rule 4).
 */
public class Customer {

    private final UUID id;
    private final String email;
    private String fullName;
    private CustomerStatus status;
    private KycState kycState;
    private Instant createdAt;
    private Instant updatedAt;
    private long version;

    private Customer(UUID id, String email, String fullName, CustomerStatus status,
                     KycState kycState, Instant createdAt, Instant updatedAt, long version) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.status = status;
        this.kycState = kycState;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    /** Reconstitutes an aggregate from persistence (versioned for optimistic locking). */
    public static Customer restore(UUID id, String email, String fullName, CustomerStatus status,
                                   KycState kycState, Instant createdAt, Instant updatedAt, long version) {
        return new Customer(id, email, fullName, status, kycState, createdAt, updatedAt, version);
    }

    /** A newly onboarded customer starts in PENDING / NOT_STARTED. */
    public static Customer onboard(String email, String fullName) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("fullName must not be blank");
        }
        Instant now = Instant.now();
        return new Customer(UUID.randomUUID(), email.toLowerCase(), fullName.trim(),
                CustomerStatus.PENDING, KycState.NOT_STARTED, now, now, 0L);
    }

    public Customer activate() {
        return transition(CustomerStatus.ACTIVE, "activate");
    }

    public Customer suspend() {
        return transition(CustomerStatus.SUSPENDED, "suspend");
    }

    public Customer block() {
        return transition(CustomerStatus.BLOCKED, "block");
    }

    public Customer close() {
        return transition(CustomerStatus.CLOSED, "close");
    }

    public Customer startKyc() {
        return transitionKyc(KycState.PENDING, "start KYC");
    }

    public Customer approveKyc() {
        return transitionKyc(KycState.APPROVED, "approve KYC");
    }

    public Customer rejectKyc() {
        return transitionKyc(KycState.REJECTED, "reject KYC");
    }

    private Customer transition(CustomerStatus target, String action) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateTransitionException(
                    "Cannot " + action + " a customer from " + status + " to " + target);
        }
        this.status = target;
        this.updatedAt = Instant.now();
        return this;
    }

    private Customer transitionKyc(KycState target, String action) {
        if (!kycState.canTransitionTo(target)) {
            throw new IllegalStateTransitionException(
                    "Cannot " + action + " from KYC state " + kycState + " to " + target);
        }
        this.kycState = target;
        this.updatedAt = Instant.now();
        return this;
    }

    public UUID id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String fullName() {
        return fullName;
    }

    public CustomerStatus status() {
        return status;
    }

    public KycState kycState() {
        return kycState;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public long version() {
        return version;
    }
}
