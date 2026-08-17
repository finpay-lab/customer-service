package com.finpay.customer.service.application;

/** Actions understood by the customer lifecycle state machine. */
public enum CustomerStatusAction {
    ACTIVATE,
    SUSPEND,
    BLOCK,
    CLOSE
}
