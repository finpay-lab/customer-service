package com.finpay.customer.service.application;

import com.finpay.customer.service.domain.event.CustomerStatusChangedEvent;
import com.finpay.customer.service.domain.exception.CustomerNotFoundException;
import com.finpay.customer.service.domain.model.Customer;
import com.finpay.customer.service.domain.model.CustomerStatus;
import com.finpay.customer.service.domain.repository.CustomerRepository;
import com.finpay.customer.service.domain.repository.OutboxRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies an action to the customer lifecycle state machine. Illegal moves are
 * rejected by the aggregate (AGENTS.md rule 9); the status-change event is
 * appended to the outbox in the same transaction.
 */
@org.springframework.stereotype.Service
public class ChangeCustomerStatusUseCase {

    private final CustomerRepository customerRepository;
    private final OutboxRepository outboxRepository;

    public ChangeCustomerStatusUseCase(CustomerRepository customerRepository, OutboxRepository outboxRepository) {
        this.customerRepository = customerRepository;
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public CustomerStatus change(ChangeCustomerStatusCommand command) {
        Customer customer = customerRepository.findById(command.customerId())
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Customer " + command.customerId() + " does not exist"));
        CustomerStatus previous = customer.status();
        switch (command.action()) {
            case ACTIVATE -> customer.activate();
            case SUSPEND -> customer.suspend();
            case BLOCK -> customer.block();
            case CLOSE -> customer.close();
        }
        if (customer.status() != previous) {
            customerRepository.save(customer);
            outboxRepository.save(CustomerStatusChangedEvent.of(customer, previous));
        }
        return customer.status();
    }
}
