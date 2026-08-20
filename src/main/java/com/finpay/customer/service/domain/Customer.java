package com.finpay.customer.service.domain;

import java.time.Instant;
import java.util.List;

/**
 * Customer profile aggregate (FP-31). Encapsulates onboarding state machine
 * (Rule 9) and idempotent onboarding (Rule 6).
 */
public class Customer {

    public enum OnboardingState { PENDING, VERIFIED, ACTIVE, SUSPENDED, CLOSED }

    private final String customerId;
    private String fullName;
    private String email;
    private OnboardingState state;
    private final Instant createdAt;
    private Instant updatedAt;

    public Customer(String customerId, String fullName, String email) {
        this.customerId = customerId;
        this.fullName = fullName;
        this.email = email;
        this.state = OnboardingState.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String customerId() { return customerId; }
    public String fullName() { return fullName; }
    public String email() { return email; }
    public OnboardingState state() { return state; }
    public Instant createdAt() { return createdAt; }

    /** Rule 6: idempotent onboarding — reprocessing same data is a no-op. */
    public void onboard(String name, String mail) {
        if (state == OnboardingState.CLOSED) throw new IllegalStateException("closed");
        this.fullName = name;
        this.email = mail;
        // PENDING -> VERIFIED (KYC hook would gate ACTIVE)
        if (state == OnboardingState.PENDING) this.state = OnboardingState.VERIFIED;
        this.updatedAt = Instant.now();
    }

    /** Rule 9: only from VERIFIED -> ACTIVE. */
    public void activate() {
        if (state != OnboardingState.VERIFIED)
            throw new IllegalStateTransition(state, "ACTIVE");
        this.state = OnboardingState.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void suspend() {
        if (state != OnboardingState.ACTIVE)
            throw new IllegalStateTransition(state, "SUSPENDED");
        this.state = OnboardingState.SUSPENDED;
        this.updatedAt = Instant.now();
    }

    public static final class IllegalStateTransition extends RuntimeException {
        IllegalStateTransition(OnboardingState from, String to) {
            super("cannot transition " + from + " -> " + to);
        }
    }
}
