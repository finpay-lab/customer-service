package com.finpay.customer.service.infrastructure.resilience;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Tiny retry-with-fixed-backoff helper (Rule 8). Wrapped by a CircuitBreaker:
 * the breaker decides whether to attempt at all; this decides how hard to try
 * before giving up.
 */
public final class Retry {

    private Retry() {
    }

    /** Retries the action up to {@code maxAttempts} times total, sleeping {@code backoff} between attempts. */
    public static <T> T withBackoff(int maxAttempts, Duration backoff, Supplier<T> action) {
        int attempt = 0;
        while (true) {
            try {
                return action.get();
            } catch (RuntimeException e) {
                attempt++;
                if (attempt >= maxAttempts) {
                    throw e;
                }
                sleep(backoff);
            }
        }
    }

    private static void sleep(Duration backoff) {
        try {
            Thread.sleep(backoff.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during retry backoff", e);
        }
    }
}