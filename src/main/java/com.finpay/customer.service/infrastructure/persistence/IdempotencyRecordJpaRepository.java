package com.finpay.customer.service.infrastructure.persistence;

import com.finpay.customer.service.domain.model.IdempotencyRecord;
import com.finpay.customer.service.domain.repository.IdempotencyRecordRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * JPA-backed idempotency store. Concurrency is handled by the DB unique
 * constraint via {@code INSERT ... ON CONFLICT DO NOTHING}.
 */
@Repository
public class IdempotencyRecordJpaRepository implements IdempotencyRecordRepository {

    private final IdempotencySpringRepository springRepository;

    public IdempotencyRecordJpaRepository(IdempotencySpringRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    public Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey) {
        return springRepository.findById(idempotencyKey).map(e ->
                new IdempotencyRecord(e.getIdempotencyKey(), e.getPayloadHash(), e.getCustomerId(), e.getCreatedAt()));
    }

    @Override
    public boolean insertIfAbsent(IdempotencyRecord record) {
        return springRepository.insertIfAbsent(record.idempotencyKey(), record.payloadHash(),
                record.customerId(), Instant.now()) > 0;
    }
}