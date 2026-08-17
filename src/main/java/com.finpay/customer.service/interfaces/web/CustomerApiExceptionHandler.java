package com.finpay.customer.service.interfaces.web;

import com.finpay.common.web.error.ErrorCode;
import com.finpay.common.web.error.ProblemDetail;
import com.finpay.customer.service.domain.exception.CustomerAlreadyExistsException;
import com.finpay.customer.service.domain.exception.CustomerNotFoundException;
import com.finpay.customer.service.domain.exception.IdempotencyConflictException;
import com.finpay.customer.service.domain.exception.IllegalStateTransitionException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Maps domain/validation failures to RFC-9457 problem responses with stable
 * machine-readable codes (common-web). Internal details never leak.
 */
@RestControllerAdvice
public class CustomerApiExceptionHandler {

    private static final String MDC_KEY = "correlationId";

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ProblemDetail> notFound(CustomerNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, ErrorCode.CUSTOMER_NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ProblemDetail> idempotencyConflict(IdempotencyConflictException e) {
        return problem(HttpStatus.CONFLICT, ErrorCode.IDEMPOTENCY_CONFLICT, e.getMessage());
    }

    @ExceptionHandler(CustomerAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> alreadyExists(CustomerAlreadyExistsException e) {
        return problem(HttpStatus.CONFLICT, "CUSTOMER_ALREADY_EXISTS", e.getMessage());
    }

    @ExceptionHandler(IllegalStateTransitionException.class)
    public ResponseEntity<ProblemDetail> illegalTransition(IllegalStateTransitionException e) {
        return problem(HttpStatus.CONFLICT, ErrorCode.INVALID_STATE_TRANSITION, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException e) {
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> badRequest(IllegalArgumentException e) {
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", e.getMessage());
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, ErrorCode code, String message) {
        return problem(status, code.name(), message);
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
                .body(new ProblemDetail(status.value(), code, message, MDC.get(MDC_KEY), Map.of()));
    }
}