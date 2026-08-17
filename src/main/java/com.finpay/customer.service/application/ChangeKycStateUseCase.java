package com.finpay.customer.service.application;

import com.finpay.customer.service.domain.event.KycStateChangedEvent;
import com.finpay.customer.service.domain.exception.CustomerNotFoundException;
import com.finpay.customer.service.domain.model.Customer;
import com.finpay.customer.service.domain.model.KycState;
import com.finpay.customer.service.domain.repository.CustomerRepository;
import com.finpay.customer.service.domain.repository.OutboxRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies a KYC workflow action to the KYC state machine. Illegal moves are
 * rejected by the aggregate (AGENTS.md rule 9); the KYC-change event is
 * appended to the outbox in the same transaction.
 */
@org.springframework.stereotype.Service
public class ChangeKycStateUseCase {

    private final CustomerRepository customerRepository;
    private final OutboxRepository outboxRepository;

    public ChangeKycStateUseCase(CustomerRepository customerRepository, OutboxRepository outboxRepository) {
        this.customerRepository = customerRepository;
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public KycState change(ChangeKycStateCommand command) {
        Customer customer = customerRepository.findById(command.customerId())
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Customer " + command.customerId() + " does not exist"));
        KycState previous = customer.kycState();
        switch (command.action()) {
            case START_KYC -> customer.startKyc();
            case APPROVE_KYC -> customer.approveKyc();
            case REJECT_KYC -> customer.rejectKyc();
        }
        if (customer.kycState() != previous) {
            customerRepository.save(customer);
            outboxRepository.save(KycStateChangedEvent.of(customer, previous));
        }
        return customer.kycState();
    }
}
