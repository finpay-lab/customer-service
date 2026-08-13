package com.finpay.customer.application;

import com.finpay.customer.application.port.OutboxAppender;
import com.finpay.customer.domain.model.Customer;
import com.finpay.customer.domain.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/** Revokes/re-queues KYC: ACTIVE|FROZEN -&gt; KYC_PENDING and publishes {@code CustomerKycChanged}. */
@Service
public class RevokeKycUseCase {

    private final CustomerRepository customers;
    private final OutboxAppender outbox;

    public RevokeKycUseCase(CustomerRepository customers, OutboxAppender outbox) {
        this.customers = customers;
        this.outbox = outbox;
    }

    @Transactional
    public Customer execute(UUID customerId) {
        Customer customer = customers.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
        customer.revokeKyc(Instant.now());
        customers.save(customer);
        customer.domainEvents().forEach(outbox::append);
        return customer;
    }
}
