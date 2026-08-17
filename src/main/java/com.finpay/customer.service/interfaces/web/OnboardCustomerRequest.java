package com.finpay.customer.service.interfaces.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Onboarding request body. The idempotency key travels in the {@code Idempotency-Key} header. */
public record OnboardCustomerRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 255) String fullName) {
}