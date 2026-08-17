package com.finpay.customer.service.testutil;

import com.finpay.customer.service.domain.model.Customer;
import com.finpay.customer.service.domain.repository.CustomerRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** In-memory repository for unit tests (no Spring, no DB). */
public class FakeCustomerRepository implements CustomerRepository {

    private final Map<UUID, Customer> customers = new HashMap<>();

    @Override
    public void save(Customer customer) {
        customers.put(customer.id(), customer);
    }

    @Override
    public Optional<Customer> findById(UUID customerId) {
        return Optional.ofNullable(customers.get(customerId));
    }

    public int size() {
        return customers.size();
    }
}