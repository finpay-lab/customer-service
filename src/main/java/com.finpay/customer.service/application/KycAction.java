package com.finpay.customer.service.application;

/** KYC workflow actions understood by the KYC state machine. */
public enum KycAction {
    START_KYC,
    APPROVE_KYC,
    REJECT_KYC
}
