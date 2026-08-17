package com.finpay.customer.service.testutil;

import com.finpay.customer.service.domain.model.IdempotencyRecord;
import com.finpay.customer.service.domain.repository.IdempotencyRecordRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** In-memory idempotency store for unit tests. */
public class FakeIdempotencyRecordRepository implements IdempotencyRecordRepository {

    private final Map<String, IdempotencyRecord> records = new HashMap<>();

    @Override
    public Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey) {
        return Optional.ofNullable(records.get(idempotencyKey));
    }

    @Override
    public boolean insertIfAbsent(IdempotencyRecord record) {
        return records.putIfAbsent(record.idempotencyKey(), record) == null;
    }
}