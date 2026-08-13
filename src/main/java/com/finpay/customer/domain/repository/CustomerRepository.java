package com.finpay.customer.domain.repository;

import com.finpay.customer.domain.model.Customer;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence contract for the {@link Customer} aggregate. Implementations live
 * in {@code infrastructure/} (AGENTS.md rule 4).
 */
public interface CustomerRepository {

    Customer save(Customer customer);

    Optional<Customer> findById(UUID customerId);

    Optional<Customer> findByIdempotencyKey(String idempotencyKey);
}
