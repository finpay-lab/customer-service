package com.finpay.customer.service.interfaces.web;

import com.finpay.customer.service.application.CustomerStatusAction;
import jakarta.validation.constraints.NotNull;

public record ChangeCustomerStatusRequest(@NotNull CustomerStatusAction action) {
}