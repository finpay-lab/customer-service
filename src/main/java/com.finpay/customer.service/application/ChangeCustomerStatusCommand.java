package com.finpay.customer.service.application;

import java.util.UUID;

public record ChangeCustomerStatusCommand(UUID customerId, CustomerStatusAction action) {
}
