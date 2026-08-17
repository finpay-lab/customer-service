package com.finpay.customer.service.application;

import com.finpay.customer.service.domain.event.CustomerStatusChangedEvent;
import com.finpay.customer.service.domain.event.KycStateChangedEvent;
import com.finpay.customer.service.domain.exception.CustomerNotFoundException;
import com.finpay.customer.service.domain.exception.IllegalStateTransitionException;
import com.finpay.customer.service.domain.model.Customer;
import com.finpay.customer.service.domain.model.CustomerStatus;
import com.finpay.customer.service.domain.model.KycState;
import com.finpay.customer.service.testutil.FakeCustomerRepository;
import com.finpay.customer.service.testutil.FakeOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChangeStateUseCasesTest {

    private final FakeCustomerRepository customers = new FakeCustomerRepository();
    private final FakeOutboxRepository outbox = new FakeOutboxRepository();
    private final ChangeCustomerStatusUseCase statusUseCase = new ChangeCustomerStatusUseCase(customers, outbox);
    private final ChangeKycStateUseCase kycUseCase = new ChangeKycStateUseCase(customers, outbox);

    private UUID customerId;

    @BeforeEach
    void seedCustomer() {
        Customer customer = Customer.onboard("ada@example.com", "Ada Lovelace");
        customers.save(customer);
        customerId = customer.id();
    }

    @Test
    void activate_changes_status_and_appends_outbox_event() {
        CustomerStatus status = statusUseCase.change(new ChangeCustomerStatusCommand(customerId, CustomerStatusAction.ACTIVATE));

        assertThat(status).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(outbox.events()).hasSize(1);
        assertThat(outbox.events().get(0)).isInstanceOf(CustomerStatusChangedEvent.class);
    }

    @Test
    void illegal_transition_is_rejected_and_nothing_is_written() {
        assertThatThrownBy(() -> statusUseCase.change(
                new ChangeCustomerStatusCommand(customerId, CustomerStatusAction.SUSPEND)))
                .isInstanceOf(IllegalStateTransitionException.class);
        assertThat(outbox.events()).isEmpty();
    }

    @Test
    void missing_customer_is_rejected() {
        assertThatThrownBy(() -> statusUseCase.change(
                new ChangeCustomerStatusCommand(UUID.randomUUID(), CustomerStatusAction.ACTIVATE)))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void kyc_workflow_appends_kyc_outbox_event() {
        KycState state = kycUseCase.change(new ChangeKycStateCommand(customerId, KycAction.START_KYC));
        assertThat(state).isEqualTo(KycState.PENDING);
        assertThat(outbox.events()).hasSize(1);
        assertThat(outbox.events().get(0)).isInstanceOf(KycStateChangedEvent.class);
    }
}