package com.finpay.customer.service.domain.repository;

import com.finpay.customer.service.domain.model.IdempotencyRecord;

import java.util.Optional;

/**
 * Idempotency-key store (AGENTS.md rule 6). Persisted in the same transaction
 * as the aggregate so the key and the resulting resource are atomic.
 */
public interface IdempotencyRecordRepository {

    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

    /**
     * Inserts the record if the key is free (DB-level unique constraint,
     * race-safe via INSERT ... ON CONFLICT). Returns false if a concurrent
     * request already recorded the same key.
     */
    boolean insertIfAbsent(IdempotencyRecord record);
}
