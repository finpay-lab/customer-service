package com.finpay.customer.service.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOutboxJpaRepository extends JpaRepository<CustomerOutboxEntity, String> {
    java.util.List<CustomerOutboxEntity> findBySentFalseOrderByCreatedAtAsc();
}
