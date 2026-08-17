package com.finpay.customer.service.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CustomerSpringRepository extends JpaRepository<CustomerJpaEntity, UUID> {
}