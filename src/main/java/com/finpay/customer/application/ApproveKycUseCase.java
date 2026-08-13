package com.finpay.customer.application;

import com.finpay.customer.application.port.OutboxAppender;
import com.finpay.customer.domain.model.Customer;
import com.finpay.customer.domain.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/** Approves KYC: moves the customer KYC_PENDING -&gt; ACTIVE and publishes {@code CustomerKycChanged}. */
@Service
public class ApproveKycUseCase {

    private final CustomerRepository customers;
    private final OutboxAppender outbox;

    public ApproveKycUseCase(CustomerRepository customers, OutboxAppender outbox) {
        this.customers = customers;
        this.outbox = outbox;
    }

    @Transactional
    public Customer execute(UUID customerId) {
        Customer customer = customers.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
        customer.approveKyc(Instant.now());
        customers.save(customer);
        customer.domainEvents().forEach(outbox::append);
        return customer;
    }
}
