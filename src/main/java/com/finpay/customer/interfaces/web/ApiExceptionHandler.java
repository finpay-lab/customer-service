package com.finpay.customer.interfaces.web;

import com.finpay.common.web.error.ErrorCode;
import com.finpay.common.web.error.ProblemDetail;
import com.finpay.common.web.filter.CorrelationIdFilter;
import com.finpay.customer.application.CustomerNotFoundException;
import com.finpay.customer.application.IdempotencyConflictException;
import com.finpay.customer.domain.model.IllegalStateTransitionException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** Maps application exceptions to RFC-9457 problem responses (common-web). */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalStateTransitionException.class)
    public ResponseEntity<ProblemDetail> illegalStateTransition(IllegalStateTransitionException ex) {
        return problem(HttpStatus.CONFLICT, ErrorCode.INVALID_STATE_TRANSITION, ex.getMessage());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ProblemDetail> idempotencyConflict(IdempotencyConflictException ex) {
        return problem(HttpStatus.CONFLICT, ErrorCode.IDEMPOTENCY_CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ProblemDetail> customerNotFound(CustomerNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ErrorCode.CUSTOMER_NOT_FOUND, ex.getMessage());
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, ErrorCode code, String message) {
        ProblemDetail detail = new ProblemDetail(
                status.value(),
                code.name(),
                message,
                MDC.get(CorrelationIdFilter.MDC_KEY),
                Map.of());
        return ResponseEntity.status(status).body(detail);
    }
}
