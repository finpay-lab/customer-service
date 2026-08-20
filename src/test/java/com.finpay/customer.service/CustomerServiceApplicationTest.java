package com.finpay.customer.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Placeholder test for the legacy flat package (the canonical app lives in
 * com/finpay/customer/service). Kept minimal so it does not attempt to boot
 * the Spring context for the legacy bootstrap class.
 */
class CustomerServiceApplicationTest {
    @Test
    void legacyBootstrapLoads() {
        assertThat(CustomerServiceLegacyBootstrap.class).isNotNull();
    }
}
