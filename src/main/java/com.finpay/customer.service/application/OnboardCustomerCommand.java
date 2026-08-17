package com.finpay.customer.service.application;

/**
 * Idempotent onboarding command (AGENTS.md rule 6). The caller-supplied
 * idempotency key makes retries safe: replaying the same key + payload returns
 * the originally created customer instead of creating a duplicate.
 */
public record OnboardCustomerCommand(
        String idempotencyKey,
        String email,
        String fullName) {
}
