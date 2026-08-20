package com.finpay.customer.service.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OnboardingIdempotencyJpaRepository extends JpaRepository<OnboardingIdempotencyEntity, String> {
    java.util.Optional<OnboardingIdempotencyEntity> findByIdempotencyKey(String key);
}
