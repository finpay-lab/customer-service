package com.finpay.customer.service.domain.exception;

/** Raised when the search read-model cannot be reached to answer a request. */
public class ExplainUnavailableException extends RuntimeException {

    public ExplainUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}