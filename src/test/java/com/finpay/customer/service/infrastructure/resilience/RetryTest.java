package com.finpay.customer.service.infrastructure.resilience;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetryTest {

    @Test
    void retriesUntilSuccess() {
        AtomicInteger calls = new AtomicInteger();

        String result = Retry.withBackoff(3, Duration.ZERO, () -> {
            if (calls.incrementAndGet() < 3) {
                throw new IllegalStateException("transient");
            }
            return "done";
        });

        assertThat(result).isEqualTo("done");
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    void rethrowsWhenAttemptsExhausted() {
        assertThatThrownBy(() -> Retry.withBackoff(2, Duration.ZERO, () -> {
            throw new IllegalStateException("always");
        })).isInstanceOf(IllegalStateException.class);
    }
}