package com.finpay.customer.service.infrastructure.persistence;

import com.finpay.customer.service.domain.Customer;
import com.finpay.customer.service.domain.CustomerRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional
public class CustomerRepositoryImpl implements CustomerRepository {

    private final CustomerJpaRepository customers;
    private final OnboardingIdempotencyJpaRepository idempotency;

    public CustomerRepositoryImpl(CustomerJpaRepository customers, OnboardingIdempotencyJpaRepository idempotency) {
        this.customers = customers;
        this.idempotency = idempotency;
    }

    @Override public Optional<Customer> find(String customerId) {
        return customers.findById(customerId).map(CustomerEntity::toDomain);
    }
    @Override public Customer save(Customer c) { return customers.save(CustomerEntity.from(c)).toDomain(); }
    @Override public boolean idempotencyExists(String key) { return idempotency.existsById(key); }
    @Override public String idempotencyCustomerId(String key) {
        return idempotency.findByIdempotencyKey(key).map(OnboardingIdempotencyEntity::getCustomerId).orElse(null);
    }
    @Override public void markIdempotent(String key, String customerId) {
        idempotency.save(new OnboardingIdempotencyEntity(key, customerId));
    }
}
