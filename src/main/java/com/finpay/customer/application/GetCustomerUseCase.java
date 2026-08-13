package com.finpay.customer.application;

import com.finpay.customer.domain.model.Customer;
import com.finpay.customer.domain.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Read path: fetch a customer by id. */
@Service
public class GetCustomerUseCase {

    private final CustomerRepository customers;

    public GetCustomerUseCase(CustomerRepository customers) {
        this.customers = customers;
    }

    @Transactional(readOnly = true)
    public Customer execute(UUID customerId) {
        return customers.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }
}
