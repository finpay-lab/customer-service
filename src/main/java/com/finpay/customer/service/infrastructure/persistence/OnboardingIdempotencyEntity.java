package com.finpay.customer.service.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer_onboarding_idempotency")
public class OnboardingIdempotencyEntity {
    @Id
    private String idempotencyKey;
    private String customerId;
    public OnboardingIdempotencyEntity() {}
    public OnboardingIdempotencyEntity(String k, String c) { this.idempotencyKey = k; this.customerId = c; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String v) { this.idempotencyKey = v; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String v) { this.customerId = v; }
}
