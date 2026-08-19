package com.finpay.customer.service.infrastructure.resilience;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Minimal state-machine circuit breaker (Rule 8) for remote dependencies
 * (OpenSearch, LLM). Kept dependency-free on purpose so it can be shared by
 * both clients and tested with a mutable clock.
 *
 * <p>States:
 * <ul>
 *   <li>CLOSED — calls allowed; {@code failureThreshold} consecutive failures open it.</li>
 *   <li>OPEN — calls rejected until {@code resetTimeout} elapses.</li>
 *   <li>HALF_OPEN — a limited number of probe calls allowed; one failure re-opens,
 *       {@code halfOpenMaxCalls} successes close it.</li>
 * </ul>
 */
public final class CircuitBreaker {

    public enum State { CLOSED, OPEN, HALF_OPEN }

    private final int failureThreshold;
    private final Duration resetTimeout;
    private final int halfOpenMaxCalls;
    private final Clock clock;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicInteger halfOpenPermits = new AtomicInteger();
    private final AtomicInteger halfOpenSuccesses = new AtomicInteger();
    private volatile Instant openedAt;

    public CircuitBreaker(int failureThreshold, Duration resetTimeout, int halfOpenMaxCalls) {
        this(failureThreshold, resetTimeout, halfOpenMaxCalls, Clock.systemUTC());
    }

    CircuitBreaker(int failureThreshold, Duration resetTimeout, int halfOpenMaxCalls, Clock clock) {
        this.failureThreshold = failureThreshold;
        this.resetTimeout = resetTimeout;
        this.halfOpenMaxCalls = halfOpenMaxCalls;
        this.clock = clock;
    }

    /** Runs the action if the breaker permits; otherwise throws {@link CircuitOpenException}. */
    public <T> T execute(Supplier<T> action) throws CircuitOpenException {
        if (!canAttempt()) {
            throw new CircuitOpenException("Circuit breaker is open; dependency temporarily unavailable");
        }
        try {
            T result = action.get();
            recordSuccess();
            return result;
        } catch (RuntimeException e) {
            recordFailure();
            throw e;
        }
    }

    public State state() {
        return state.get();
    }

    private boolean canAttempt() {
        State current = state.get();
        if (current == State.OPEN) {
            if (Duration.between(openedAt, clock.instant()).compareTo(resetTimeout) < 0) {
                return false;
            }
            if (!state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                return state.get() != State.OPEN;
            }
            halfOpenPermits.set(halfOpenMaxCalls);
            halfOpenSuccesses.set(0);
            current = State.HALF_OPEN;
        }
        if (current == State.HALF_OPEN) {
            return halfOpenPermits.getAndUpdate(p -> p > 0 ? p - 1 : p) > 0;
        }
        return true;
    }

    private void recordSuccess() {
        if (state.get() == State.HALF_OPEN) {
            if (halfOpenSuccesses.incrementAndGet() >= halfOpenMaxCalls) {
                state.set(State.CLOSED);
                consecutiveFailures.set(0);
                halfOpenSuccesses.set(0);
                halfOpenPermits.set(0);
            }
        } else {
            consecutiveFailures.set(0);
        }
    }

    private void recordFailure() {
        if (state.get() == State.HALF_OPEN) {
            state.set(State.OPEN);
            openedAt = clock.instant();
            halfOpenPermits.set(0);
        } else if (state.get() == State.CLOSED && consecutiveFailures.incrementAndGet() >= failureThreshold) {
            state.set(State.OPEN);
            openedAt = clock.instant();
        }
    }
}