package com.finpay.customer.service.interfaces.web;

import com.finpay.customer.service.application.KycAction;
import jakarta.validation.constraints.NotNull;

public record ChangeKycStateRequest(@NotNull KycAction action) {
}