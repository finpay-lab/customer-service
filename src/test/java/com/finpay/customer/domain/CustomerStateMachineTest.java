package com.finpay.customer.domain;

import com.finpay.customer.domain.event.CustomerCreated;
import com.finpay.customer.domain.event.CustomerKycChanged;
import com.finpay.customer.domain.event.DomainEvent;
import com.finpay.customer.domain.model.Customer;
import com.finpay.customer.domain.model.CustomerProfile;
import com.finpay.customer.domain.model.CustomerStatus;
import com.finpay.customer.domain.model.IllegalStateTransitionException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerStateMachineTest {

    private static final CustomerProfile PROFILE =
            new CustomerProfile("Ada", "Lovelace", "ada@finpay.example", "+1-555-0100", "GB");
    private static final Instant NOW = Instant.parse("2026-08-12T06:00:00Z");

    private Customer newCustomer() {
        return Customer.create(UUID.randomUUID(), "key-" + UUID.randomUUID(), PROFILE, NOW);
    }

    @Test
    void create_starts_in_kyc_pending_with_kyc_flag_false_and_records_customer_created() {
        Customer customer = newCustomer();
        assertThat(customer.status()).isEqualTo(CustomerStatus.KYC_PENDING);
        assertThat(customer.isKycVerified()).isFalse();
        assertThat(customer.domainEvents()).singleElement()
                .isInstanceOf(CustomerCreated.class)
                .satisfies(e -> assertThat(((DomainEvent) e).eventType()).isEqualTo("CustomerCreated"));
    }

    @Test
    void approve_kyc_moves_kyc_pending_to_active_verified_and_records_kyc_changed() {
        Customer customer = newCustomer();
        customer.approveKyc(NOW);
        assertThat(customer.status()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(customer.isKycVerified()).isTrue();
        assertThat(customer.domainEvents()).hasSize(2)
                .last().isInstanceOf(CustomerKycChanged.class)
                .satisfies(e -> assertThat(((CustomerKycChanged) e).kycVerified()).isTrue());
    }

    @Test
    void legal_transitions_are_accepted() {
        assertThat(newCustomerFlowTo(CustomerStatus.ACTIVE).status()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(newCustomerFlowTo(CustomerStatus.FROZEN).status()).isEqualTo(CustomerStatus.FROZEN);
        assertThat(newCustomerFlowTo(CustomerStatus.CLOSED).status()).isEqualTo(CustomerStatus.CLOSED);
    }

    @Test
    void freeze_requires_active_and_records_no_event() {
        Customer customer = newCustomer();
        customer.approveKyc(NOW);
        int before = customer.domainEvents().size();
        customer.freeze(NOW);
        assertThat(customer.status()).isEqualTo(CustomerStatus.FROZEN);
        assertThat(customer.domainEvents()).hasSize(before);
    }

    @Test
    void unfreeze_requires_frozen() {
        Customer customer = newCustomer();
        customer.approveKyc(NOW);
        customer.freeze(NOW);
        customer.unfreeze(NOW);
        assertThat(customer.status()).isEqualTo(CustomerStatus.ACTIVE);
    }

    @Test
    void revoke_kyc_from_active_returns_to_pending_unverified_and_records_kyc_changed() {
        Customer customer = newCustomer();
        customer.approveKyc(NOW);
        customer.revokeKyc(NOW);
        assertThat(customer.status()).isEqualTo(CustomerStatus.KYC_PENDING);
        assertThat(customer.isKycVerified()).isFalse();
        assertThat(customer.domainEvents()).last().isInstanceOf(CustomerKycChanged.class)
                .satisfies(e -> assertThat(((CustomerKycChanged) e).kycVerified()).isFalse());
    }

    @Test
    void illegal_transitions_are_rejected() {
        Customer pending = newCustomer();
        assertThatThrownBy(() -> pending.freeze(NOW))
                .isInstanceOf(IllegalStateTransitionException.class);
        assertThatThrownBy(() -> pending.unfreeze(NOW))
                .isInstanceOf(IllegalStateTransitionException.class);
        assertThatThrownBy(() -> pending.revokeKyc(NOW))
                .isInstanceOf(IllegalStateTransitionException.class);

        Customer active = newCustomer();
        active.approveKyc(NOW);
        assertThatThrownBy(() -> active.approveKyc(NOW))
                .isInstanceOf(IllegalStateTransitionException.class);
        assertThatThrownBy(() -> active.unfreeze(NOW))
                .isInstanceOf(IllegalStateTransitionException.class);
        active.freeze(NOW);
        assertThatThrownBy(() -> active.freeze(NOW))
                .isInstanceOf(IllegalStateTransitionException.class);

        Customer closed = newCustomer();
        closed.close(NOW);
        assertThatThrownBy(() -> closed.close(NOW))
                .isInstanceOf(IllegalStateTransitionException.class);
        assertThatThrownBy(() -> closed.approveKyc(NOW))
                .isInstanceOf(IllegalStateTransitionException.class);
        assertThatThrownBy(() -> closed.freeze(NOW))
                .isInstanceOf(IllegalStateTransitionException.class);
        assertThatThrownBy(() -> closed.revokeKyc(NOW))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    /** Drives a fresh customer to the given legal state via allowed transitions. */
    private Customer newCustomerFlowTo(CustomerStatus target) {
        Customer customer = newCustomer();
        switch (target) {
            case KYC_PENDING -> {
            }
            case ACTIVE -> customer.approveKyc(NOW);
            case FROZEN -> {
                customer.approveKyc(NOW);
                customer.freeze(NOW);
            }
            case CLOSED -> customer.close(NOW);
        }
        return customer;
    }
}
