package com.finpay.customer.service.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Idempotency record (AGENTS.md rule 6): the key plus a hash of the request
 * payload. On replay, the hash tells us whether the retry carries the same
 * payload (safe replay) or a conflicting one (409).
 */
public record IdempotencyRecord(
        String idempotencyKey,
        String payloadHash,
        UUID customerId,
        Instant createdAt) {
}
