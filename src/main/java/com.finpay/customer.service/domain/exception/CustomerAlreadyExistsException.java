package com.finpay.customer.service.domain.exception;

/**
 * Raised when a unique business invariant (e.g. email) is violated on save.
 * Derived from the database constraint; mapped to a 409 at the API boundary.
 */
public class CustomerAlreadyExistsException extends RuntimeException {

    public CustomerAlreadyExistsException(String message) {
        super(message);
    }
}
