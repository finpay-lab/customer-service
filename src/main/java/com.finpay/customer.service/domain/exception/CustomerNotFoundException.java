package com.finpay.customer.service.domain.exception;

/** Referenced customer does not exist. */
public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(String message) {
        super(message);
    }
}
