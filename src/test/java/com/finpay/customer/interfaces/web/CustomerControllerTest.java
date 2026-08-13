package com.finpay.customer.interfaces.web;

import com.finpay.customer.application.ApproveKycUseCase;
import com.finpay.customer.application.ChangeCustomerStatusUseCase;
import com.finpay.customer.application.CreateCustomerUseCase;
import com.finpay.customer.application.GetCustomerUseCase;
import com.finpay.customer.application.RevokeKycUseCase;
import com.finpay.customer.domain.model.Customer;
import com.finpay.customer.domain.model.CustomerProfile;
import com.finpay.customer.domain.model.CustomerStatus;
import com.finpay.customer.interfaces.web.dto.ChangeCustomerStatusRequest;
import com.finpay.customer.interfaces.web.dto.CreateCustomerRequest;
import com.finpay.customer.interfaces.web.dto.CustomerResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    private static final CustomerProfile PROFILE =
            new CustomerProfile("Ada", "Lovelace", "ada@finpay.example", "+1-555-0100", "GB");

    @Mock
    private CreateCustomerUseCase createCustomer;
    @Mock
    private GetCustomerUseCase getCustomer;
    @Mock
    private ApproveKycUseCase approveKyc;
    @Mock
    private RevokeKycUseCase revokeKyc;
    @Mock
    private ChangeCustomerStatusUseCase changeStatus;

    @InjectMocks
    private CustomerController controller;

    @Test
    void create_returns_201_for_a_new_customer_and_delegates() {
        when(createCustomer.execute(any())).thenReturn(new CreateCustomerUseCase.CreateCustomerResult(sample(), true));

        ResponseEntity<CustomerResponse> response = controller.create(
                "key-1", new CreateCustomerRequest("Ada", "Lovelace", "ada@finpay.example", "+1-555-0100", "GB"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(CustomerStatus.KYC_PENDING);
        verify(createCustomer).execute(any());
    }

    @Test
    void create_returns_200_for_an_idempotent_replay() {
        when(createCustomer.execute(any())).thenReturn(new CreateCustomerUseCase.CreateCustomerResult(sample(), false));

        ResponseEntity<CustomerResponse> response = controller.create(
                "key-1", new CreateCustomerRequest("Ada", "Lovelace", "ada@finpay.example", "+1-555-0100", "GB"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void get_delegates_to_read_use_case() {
        when(getCustomer.execute(any())).thenReturn(sample());

        CustomerResponse response = controller.get(UUID.randomUUID());

        assertThat(response.id()).isNotNull();
        verify(getCustomer).execute(any());
    }

    @Test
    void freeze_delegates_to_status_use_case() {
        Customer frozen = sample();
        frozen.approveKyc(Instant.now());
        frozen.freeze(Instant.now());
        when(changeStatus.execute(any(), any())).thenReturn(frozen);

        CustomerResponse response = controller.changeStatus(
                frozen.id(), new ChangeCustomerStatusRequest(CustomerStatus.FROZEN));

        assertThat(response.status()).isEqualTo(CustomerStatus.FROZEN);
        verify(changeStatus).execute(frozen.id(), CustomerStatus.FROZEN);
    }

    @Test
    void approve_kyc_delegates_to_kyc_use_case() {
        when(approveKyc.execute(any())).thenReturn(sample());

        CustomerResponse response = controller.approveKyc(UUID.randomUUID());

        assertThat(response.id()).isNotNull();
        verify(approveKyc).execute(any());
    }

    @Test
    void revoke_kyc_delegates_to_kyc_use_case() {
        when(revokeKyc.execute(any())).thenReturn(sample());

        CustomerResponse response = controller.revokeKyc(UUID.randomUUID());

        assertThat(response.id()).isNotNull();
        verify(revokeKyc).execute(any());
    }

    private Customer sample() {
        return Customer.create(UUID.randomUUID(), "key-1", PROFILE, Instant.now());
    }
}
