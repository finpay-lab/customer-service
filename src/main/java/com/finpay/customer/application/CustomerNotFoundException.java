package com.finpay.customer.application;

import java.util.UUID;

/** Thrown when a lookup for a customer aggregate misses. Maps to HTTP 404. */
public final class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(UUID customerId) {
        super("Customer " + customerId + " does not exist");
    }
}
