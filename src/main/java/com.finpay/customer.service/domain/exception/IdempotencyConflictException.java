package com.finpay.customer.service.domain.exception;

/**
 * Raised when an idempotency key is replayed with a different payload
 * (AGENTS.md rule 6). Mapped to ErrorCode.IDEMPOTENCY_CONFLICT (409).
 */
public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String idempotencyKey) {
        super("Idempotency key '" + idempotencyKey + "' was already used with a different payload");
    }
}
