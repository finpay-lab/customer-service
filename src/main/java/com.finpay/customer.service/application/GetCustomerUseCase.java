package com.finpay.customer.service.application;

import com.finpay.customer.service.domain.exception.CustomerNotFoundException;
import com.finpay.customer.service.domain.model.Customer;
import com.finpay.customer.service.domain.repository.CustomerRepository;

import java.util.UUID;

/** Read path for the customer aggregate (no transaction needed). */
@org.springframework.stereotype.Service
public class GetCustomerUseCase {

    private final CustomerRepository customerRepository;

    public GetCustomerUseCase(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer byId(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer " + customerId + " does not exist"));
    }
}