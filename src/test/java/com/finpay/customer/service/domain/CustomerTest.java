package com.finpay.customer.service.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerTest {

    @Test
    void onboardMovesPendingToVerified() {
        Customer c = new Customer("c1", "Jane", "jane@x.com");
        assertThat(c.state()).isEqualTo(Customer.OnboardingState.PENDING);
        c.onboard("Jane Doe", "jane@x.com");
        assertThat(c.state()).isEqualTo(Customer.OnboardingState.VERIFIED);
    }

    @Test
    void activateOnlyFromVerified() {
        Customer c = new Customer("c2", "John", "john@x.com");
        c.onboard("John", "john@x.com");
        c.activate();
        assertThat(c.state()).isEqualTo(Customer.OnboardingState.ACTIVE);
        // already active -> suspend ok
        c.suspend();
        assertThat(c.state()).isEqualTo(Customer.OnboardingState.SUSPENDED);
        // cannot re-activate from suspended without going through VERIFIED (Rule 9)
        assertThatThrownBy(c::activate).isInstanceOf(Customer.IllegalStateTransition.class);
    }

    @Test
    void idempotentOnboardIsNoOp() {
        Customer c = new Customer("c3", "A", "a@x.com");
        c.onboard("A", "a@x.com");
        c.onboard("A", "a@x.com"); // reprocessing same data
        assertThat(c.state()).isEqualTo(Customer.OnboardingState.VERIFIED);
    }
}
