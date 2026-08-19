package com.finpay.customer.service.infrastructure.resilience;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CircuitBreakerTest {

    @Test
    void opensAfterFailureThresholdAndRejectsCallsWhileOpen() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-19T00:00:00Z"));
        CircuitBreaker breaker = new CircuitBreaker(2, Duration.ofSeconds(30), 2, clock);

        assertThat(breaker.execute(() -> "ok")).isEqualTo("ok");
        assertThat(breaker.state()).isEqualTo(CircuitBreaker.State.CLOSED);

        assertThatThrownBy(() -> breaker.execute(() -> fail("boom"))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> breaker.execute(() -> fail("boom"))).isInstanceOf(IllegalStateException.class);
        assertThat(breaker.state()).isEqualTo(CircuitBreaker.State.OPEN);

        assertThatThrownBy(() -> breaker.execute(() -> "blocked"))
                .isInstanceOf(CircuitOpenException.class);
        assertThat(breaker.state()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void recoversViaHalfOpenProbeAfterResetTimeout() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-19T00:00:00Z"));
        CircuitBreaker breaker = new CircuitBreaker(2, Duration.ofSeconds(30), 1, clock);

        assertThatThrownBy(() -> breaker.execute(() -> fail("boom"))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> breaker.execute(() -> fail("boom"))).isInstanceOf(IllegalStateException.class);
        assertThat(breaker.state()).isEqualTo(CircuitBreaker.State.OPEN);

        clock.advance(Duration.ofSeconds(31));
        assertThat(breaker.execute(() -> "recovered")).isEqualTo("recovered");
        assertThat(breaker.state()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void reopensWhenHalfOpenProbeFails() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-19T00:00:00Z"));
        CircuitBreaker breaker = new CircuitBreaker(2, Duration.ofSeconds(30), 1, clock);

        assertThatThrownBy(() -> breaker.execute(() -> fail("boom"))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> breaker.execute(() -> fail("boom"))).isInstanceOf(IllegalStateException.class);
        clock.advance(Duration.ofSeconds(31));

        assertThatThrownBy(() -> breaker.execute(() -> fail("boom again"))).isInstanceOf(IllegalStateException.class);
        assertThat(breaker.state()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    private static String fail(String message) {
        throw new IllegalStateException(message);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}