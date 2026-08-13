package com.finpay.customer.infrastructure.persistence;

import com.finpay.customer.domain.model.Customer;
import com.finpay.customer.domain.repository.CustomerRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** Adapter implementing the domain {@link CustomerRepository} on top of JPA. */
@Component
public class JpaCustomerRepository implements CustomerRepository {

    private final CustomerJpaRepository jpa;
    private final CustomerMapper mapper;

    public JpaCustomerRepository(CustomerJpaRepository jpa, CustomerMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public Customer save(Customer customer) {
        return mapper.toDomain(jpa.save(mapper.toEntity(customer)));
    }

    @Override
    public Optional<Customer> findById(UUID customerId) {
        return jpa.findById(customerId).map(mapper::toDomain);
    }

    @Override
    public Optional<Customer> findByIdempotencyKey(String idempotencyKey) {
        return jpa.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
    }
}
