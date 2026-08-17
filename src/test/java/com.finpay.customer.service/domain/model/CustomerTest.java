package com.finpay.customer.service.domain.model;

import com.finpay.customer.service.domain.exception.IllegalStateTransitionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerTest {

    @Test
    void onboard_starts_pending_not_started() {
        Customer customer = Customer.onboard("Ada@Example.com", "  Ada Lovelace  ");

        assertThat(customer.status()).isEqualTo(CustomerStatus.PENDING);
        assertThat(customer.kycState()).isEqualTo(KycState.NOT_STARTED);
        assertThat(customer.email()).isEqualTo("ada@example.com");
        assertThat(customer.fullName()).isEqualTo("Ada Lovelace");
        assertThat(customer.version()).isZero();
    }

    @Test
    void activate_allows_pending_and_suspended() {
        Customer pending = Customer.onboard("a@x.com", "A");
        assertThat(pending.activate().status()).isEqualTo(CustomerStatus.ACTIVE);

        Customer suspended = Customer.onboard("b@x.com", "B").activate().suspend();
        assertThat(suspended.activate().status()).isEqualTo(CustomerStatus.ACTIVE);
    }

    @Test
    void activate_rejects_active_and_blocked() {
        Customer active = Customer.onboard("a@x.com", "A").activate();
        assertThatThrownBy(active::activate)
                .isInstanceOf(IllegalStateTransitionException.class);

        Customer blocked = Customer.onboard("b@x.com", "B").activate().block();
        assertThatThrownBy(blocked::activate)
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void block_allows_pending_active_and_suspended() {
        assertThat(Customer.onboard("a@x.com", "A").block().status()).isEqualTo(CustomerStatus.BLOCKED);
        assertThat(Customer.onboard("b@x.com", "B").activate().block().status()).isEqualTo(CustomerStatus.BLOCKED);
        assertThat(Customer.onboard("c@x.com", "C").activate().suspend().block().status()).isEqualTo(CustomerStatus.BLOCKED);
    }

    @Test
    void block_rejects_blocked_and_closed() {
        Customer blocked = Customer.onboard("a@x.com", "A").block();
        assertThatThrownBy(blocked::block).isInstanceOf(IllegalStateTransitionException.class);

        Customer closed = Customer.onboard("b@x.com", "B").close();
        assertThatThrownBy(closed::block).isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void suspend_requires_active() {
        Customer pending = Customer.onboard("a@x.com", "A");
        assertThatThrownBy(pending::suspend).isInstanceOf(IllegalStateTransitionException.class);

        Customer active = pending.activate();
        assertThat(active.suspend().status()).isEqualTo(CustomerStatus.SUSPENDED);
    }

    @Test
    void close_is_terminal_and_allowed_from_any_open_status() {
        Customer pending = Customer.onboard("a@x.com", "A").close();
        assertThat(pending.status()).isEqualTo(CustomerStatus.CLOSED);

        Customer closed = Customer.onboard("b@x.com", "B").activate().close();
        assertThatThrownBy(closed::close).isInstanceOf(IllegalStateTransitionException.class);
        assertThatThrownBy(closed::activate).isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void kyc_transitions_follow_legal_table() {
        Customer c = Customer.onboard("a@x.com", "A");
        assertThat(c.startKyc().kycState()).isEqualTo(KycState.PENDING);
        assertThat(c.approveKyc().kycState()).isEqualTo(KycState.APPROVED);
        assertThatThrownBy(c::approveKyc).isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void kyc_rejected_can_be_restarted() {
        Customer c = Customer.onboard("a@x.com", "A").startKyc().rejectKyc();
        assertThat(c.kycState()).isEqualTo(KycState.REJECTED);
        assertThat(c.startKyc().kycState()).isEqualTo(KycState.PENDING);
    }

    @Test
    void kyc_cannot_approve_before_start() {
        Customer c = Customer.onboard("a@x.com", "A");
        assertThatThrownBy(c::approveKyc).isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void restore_preserves_aggregate_state() {
        Customer original = Customer.onboard("a@x.com", "A").activate();
        Customer restored = Customer.restore(
                original.id(), original.email(), original.fullName(),
                original.status(), original.kycState(),
                original.createdAt(), original.updatedAt(), original.version());

        assertThat(restored.status()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(restored.id()).isEqualTo(original.id());
    }
}