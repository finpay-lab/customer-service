package com.finpay.customer.service.infrastructure.persistence;

import com.finpay.customer.service.domain.Customer;
import com.finpay.customer.service.domain.Customer.OnboardingState;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "customers")
public class CustomerEntity {

    @Id
    private String customerId;
    private String fullName;
    private String email;
    private String state;
    private Instant createdAt;
    private Instant updatedAt;

    public CustomerEntity() {}

    public static CustomerEntity from(Customer c) {
        CustomerEntity e = new CustomerEntity();
        e.customerId = c.customerId();
        e.fullName = c.fullName();
        e.email = c.email();
        e.state = c.state().name();
        e.createdAt = c.createdAt();
        e.updatedAt = Instant.now();
        return e;
    }

    public Customer toDomain() {
        Customer c = new Customer(customerId, fullName, email);
        // restore state without re-violating invariants
        if (state.equals("VERIFIED")) { /* already verified */ }
        else if (state.equals("ACTIVE")) { c.activate(); }
        else if (state.equals("SUSPENDED")) { c.activate(); c.suspend(); }
        else if (state.equals("CLOSED")) { /* closed restore best-effort */ }
        return c;
    }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String v) { this.customerId = v; }
    public String getFullName() { return fullName; }
    public void setFullName(String v) { this.fullName = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getState() { return state; }
    public void setState(String v) { this.state = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
