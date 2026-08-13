package com.finpay.customer.interfaces.web.dto;

import com.finpay.customer.domain.model.CustomerStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Body of {@code PATCH /api/v1/customers/{id}/status}. Only the pure status
 * transitions FROZEN / ACTIVE / CLOSED are valid targets; KYC_PENDING is
 * rejected (KYC changes go through the dedicated KYC endpoints).
 */
public record ChangeCustomerStatusRequest(@NotNull CustomerStatus status) {
}
