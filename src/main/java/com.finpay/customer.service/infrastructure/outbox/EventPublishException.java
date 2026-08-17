package com.finpay.customer.service.infrastructure.outbox;

/** Raised when an outbox row could not be delivered; the relay retries later. */
public class EventPublishException extends RuntimeException {

    public EventPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}