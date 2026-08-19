package com.finpay.customer.service.interfaces.web.explain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** POST /explain request body. */
public record ExplainRequest(
        @NotNull(message = "customerId is required") UUID customerId,
        @NotBlank(message = "query must not be blank") String query) {
}