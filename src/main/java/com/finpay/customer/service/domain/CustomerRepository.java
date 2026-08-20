package com.finpay.customer.service.domain;

import java.util.Optional;

/** Domain port for customer persistence (Rule 4: no JPA imports). */
public interface CustomerRepository {
    Optional<Customer> find(String customerId);
    Customer save(Customer customer);
    boolean idempotencyExists(String key);
    String idempotencyCustomerId(String key);
    void markIdempotent(String key, String customerId);
}
