package com.finpay.customer.service.domain.repository;

import com.finpay.customer.service.domain.model.Customer;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for the {@link Customer} aggregate. Interface lives in the domain
 * (AGENTS.md rule 4); the JPA implementation lives in infrastructure.
 */
public interface CustomerRepository {

    void save(Customer customer);

    Optional<Customer> findById(UUID customerId);
}
