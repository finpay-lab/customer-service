package com.finpay.customer.service.application;

import java.util.UUID;

public record ChangeKycStateCommand(UUID customerId, KycAction action) {
}
