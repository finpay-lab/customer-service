package com.finpay.customer.interfaces.web;

import com.finpay.customer.application.CustomerNotFoundException;
import com.finpay.customer.application.IdempotencyConflictException;
import com.finpay.customer.domain.model.CustomerStatus;
import com.finpay.customer.domain.model.IllegalStateTransitionException;
import com.finpay.common.web.error.ProblemDetail;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void illegal_transition_maps_to_409_invalid_state_transition() {
        ResponseEntity<ProblemDetail> response = handler.illegalStateTransition(
                new IllegalStateTransitionException(
                        UUID.randomUUID(), "freeze", CustomerStatus.KYC_PENDING,
                        new CustomerStatus[]{CustomerStatus.ACTIVE}));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo("INVALID_STATE_TRANSITION");
        assertThat(response.getBody().status()).isEqualTo(409);
    }

    @Test
    void idempotency_conflict_maps_to_409_idempotency_conflict() {
        ResponseEntity<ProblemDetail> response = handler.idempotencyConflict(
                new IdempotencyConflictException("key-1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo("IDEMPOTENCY_CONFLICT");
    }

    @Test
    void missing_customer_maps_to_404_customer_not_found() {
        ResponseEntity<ProblemDetail> response = handler.customerNotFound(
                new CustomerNotFoundException(UUID.randomUUID()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().code()).isEqualTo("CUSTOMER_NOT_FOUND");
    }
}
