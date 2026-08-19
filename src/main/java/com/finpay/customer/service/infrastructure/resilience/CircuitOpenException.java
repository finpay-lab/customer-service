package com.finpay.customer.service.infrastructure.resilience;

import java.time.Duration;
import java.util.function.Supplier;

/** Raised when a circuit-breaker rejects a call before it reaches the dependency. */
public class CircuitOpenException extends RuntimeException {

    public CircuitOpenException(String message) {
        super(message);
    }
}