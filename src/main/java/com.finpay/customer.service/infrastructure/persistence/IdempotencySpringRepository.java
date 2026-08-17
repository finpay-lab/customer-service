package com.finpay.customer.service.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface IdempotencySpringRepository extends JpaRepository<IdempotencyRecordJpaEntity, String> {

    @Modifying
    @Query(value = """
            insert into idempotency_key (idempotency_key, payload_hash, customer_id, created_at)
            values (:key, :payloadHash, :customerId, :createdAt)
            on conflict (idempotency_key) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("key") String key,
                       @Param("payloadHash") String payloadHash,
                       @Param("customerId") UUID customerId,
                       @Param("createdAt") Instant createdAt);
}