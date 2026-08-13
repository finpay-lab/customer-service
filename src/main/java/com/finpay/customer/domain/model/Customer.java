package com.finpay.customer.domain.model;

import com.finpay.customer.domain.event.CustomerCreated;
import com.finpay.customer.domain.event.CustomerKycChanged;
import com.finpay.customer.domain.event.DomainEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Customer aggregate root. Owns the profile, the status state machine
 * (ACTIVE / FROZEN / KYC_PENDING / CLOSED) and the KYC verification flag.
 * Illegal transitions are rejected ({@link IllegalStateTransitionException}).
 *
 * <p>Pure domain object: no Spring/JPA/Kafka dependencies (AGENTS.md rule 4).
 * Events are recorded on the aggregate and flushed to the transactional outbox
 * by the application layer.
 */
public final class Customer {

    private final UUID id;
    private final String idempotencyKey;
    private final CustomerProfile profile;
    private CustomerStatus status;
    private boolean kycVerified;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Customer(
            UUID id,
            String idempotencyKey,
            CustomerProfile profile,
            CustomerStatus status,
            boolean kycVerified,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.profile = profile;
        this.status = status;
        this.kycVerified = kycVerified;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Creates a new customer in the initial {@link CustomerStatus#KYC_PENDING}
     * state (KYC not yet verified) and records a {@link CustomerCreated} event.
     */
    public static Customer create(UUID id, String idempotencyKey, CustomerProfile profile, Instant now) {
        Customer customer = new Customer(
                id, idempotencyKey, profile, CustomerStatus.KYC_PENDING, false, now, now);
        customer.domainEvents.add(CustomerCreated.of(customer));
        return customer;
    }

    /** Rebuilds an aggregate from persistence; does not record events. */
    public static Customer reconstruct(
            UUID id,
            String idempotencyKey,
            CustomerProfile profile,
            CustomerStatus status,
            boolean kycVerified,
            Instant createdAt,
            Instant updatedAt) {
        return new Customer(id, idempotencyKey, profile, status, kycVerified, createdAt, updatedAt);
    }

    /** Approves KYC: KYC_PENDING -&gt; ACTIVE. Illegal from any other state. */
    public void approveKyc(Instant now) {
        requireFrom("approve KYC", CustomerStatus.KYC_PENDING);
        this.kycVerified = true;
        this.status = CustomerStatus.ACTIVE;
        this.updatedAt = now;
        this.domainEvents.add(CustomerKycChanged.of(this, now));
    }

    /** Revokes/re-queues KYC verification: ACTIVE|FROZEN -&gt; KYC_PENDING. */
    public void revokeKyc(Instant now) {
        requireFrom("revoke KYC", CustomerStatus.ACTIVE, CustomerStatus.FROZEN);
        this.kycVerified = false;
        this.status = CustomerStatus.KYC_PENDING;
        this.updatedAt = now;
        this.domainEvents.add(CustomerKycChanged.of(this, now));
    }

    /** Freezes an active customer: ACTIVE -&gt; FROZEN. */
    public void freeze(Instant now) {
        requireFrom("freeze", CustomerStatus.ACTIVE);
        this.status = CustomerStatus.FROZEN;
        this.updatedAt = now;
    }

    /** Unfreezes a frozen customer: FROZEN -&gt; ACTIVE. */
    public void unfreeze(Instant now) {
        requireFrom("unfreeze", CustomerStatus.FROZEN);
        this.status = CustomerStatus.ACTIVE;
        this.updatedAt = now;
    }

    /** Closes the customer record: ACTIVE|FROZEN|KYC_PENDING -&gt; CLOSED (terminal). */
    public void close(Instant now) {
        requireFrom("close", CustomerStatus.ACTIVE, CustomerStatus.FROZEN, CustomerStatus.KYC_PENDING);
        this.status = CustomerStatus.CLOSED;
        this.updatedAt = now;
    }

    private void requireFrom(String operation, CustomerStatus... legalFrom) {
        for (CustomerStatus legal : legalFrom) {
            if (this.status == legal) {
                return;
            }
        }
        throw new IllegalStateTransitionException(id, operation, status, legalFrom);
    }

    public UUID id() {
        return id;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public CustomerProfile profile() {
        return profile;
    }

    public CustomerStatus status() {
        return status;
    }

    public boolean isKycVerified() {
        return kycVerified;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    /** Same profile attributes as the given profile (idempotency conflict check). */
    public boolean sameProfile(CustomerProfile other) {
        return profile.equals(other);
    }

    /** Events recorded since the aggregate was created/reconstructed. */
    public List<DomainEvent> domainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }
}
