package com.finpay.customer.application;

import com.finpay.customer.application.port.OutboxAppender;
import com.finpay.customer.domain.model.Customer;
import com.finpay.customer.domain.model.CustomerStatus;
import com.finpay.customer.domain.model.IllegalStateTransitionException;
import com.finpay.customer.domain.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Applies a pure status transition (freeze / unfreeze / close) requested via
 * the status endpoint. KYC approval/revocation are intentionally NOT reachable
 * here: KYC_PENDING and the KYC flag are only changed through the KYC endpoints,
 * which keeps the two concerns from being conflated.
 */
@Service
public class ChangeCustomerStatusUseCase {

    private final CustomerRepository customers;
    private final OutboxAppender outbox;

    public ChangeCustomerStatusUseCase(CustomerRepository customers, OutboxAppender outbox) {
        this.customers = customers;
        this.outbox = outbox;
    }

    @Transactional
    public Customer execute(UUID customerId, CustomerStatus target) {
        Customer customer = customers.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
        Instant now = Instant.now();
        switch (target) {
            case FROZEN -> customer.freeze(now);
            case ACTIVE -> customer.unfreeze(now);
            case CLOSED -> customer.close(now);
            case KYC_PENDING -> throw new IllegalStateTransitionException(
                    customerId, "change status to KYC_PENDING", customer.status(),
                    new CustomerStatus[]{CustomerStatus.ACTIVE, CustomerStatus.FROZEN});
        }
        customers.save(customer);
        customer.domainEvents().forEach(outbox::append);
        return customer;
    }
}
