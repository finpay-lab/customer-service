package com.finpay.customer.application;

/** Thrown when an idempotency key is reused with a different request payload. */
public final class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String idempotencyKey) {
        super("Request with idempotency key " + idempotencyKey
                + " already exists with a different payload");
    }
}
