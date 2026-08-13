package com.finpay.customer.application;

import com.finpay.customer.application.port.OutboxAppender;
import com.finpay.customer.domain.model.Customer;
import com.finpay.customer.domain.model.CustomerProfile;
import com.finpay.customer.domain.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Idempotent customer creation (AGENTS.md rule 6). The idempotency key is the
 * unique business key of the aggregate: replaying the same key returns the
 * original customer instead of creating a second one. The customer row and the
 * outbox {@code CustomerCreated} row commit in the same transaction (ADR-0004);
 * a replay never enqueues a duplicate event.
 */
@Service
public class CreateCustomerUseCase {

    private final CustomerRepository customers;
    private final OutboxAppender outbox;

    public CreateCustomerUseCase(CustomerRepository customers, OutboxAppender outbox) {
        this.customers = customers;
        this.outbox = outbox;
    }

    @Transactional
    public CreateCustomerResult execute(CreateCustomerCommand command) {
        Customer existing = customers.findByIdempotencyKey(command.idempotencyKey()).orElse(null);
        if (existing != null) {
            if (!existing.sameProfile(command.profile())) {
                throw new IdempotencyConflictException(command.idempotencyKey());
            }
            return new CreateCustomerResult(existing, false);
        }

        Customer customer = Customer.create(
                UUID.randomUUID(), command.idempotencyKey(), command.profile(), Instant.now());
        customers.save(customer);
        customer.domainEvents().forEach(outbox::append);
        return new CreateCustomerResult(customer, true);
    }

    public record CreateCustomerCommand(String idempotencyKey, CustomerProfile profile) {
    }

    public record CreateCustomerResult(Customer customer, boolean created) {
    }
}
