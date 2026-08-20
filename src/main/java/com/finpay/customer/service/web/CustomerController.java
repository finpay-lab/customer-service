package com.finpay.customer.service.web;

import com.finpay.customer.service.domain.Customer;
import com.finpay.customer.service.domain.CustomerRepository;
import com.finpay.customer.service.domain.CustomerOutbox;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Transport <-> use case only (Rule 3). Idempotent onboarding (Rule 6). */
@RestController
@RequestMapping("/v1/customers")
public class CustomerController {

    private final CustomerRepository repository;
    private final CustomerOutbox outbox;

    public CustomerController(CustomerRepository repository, CustomerOutbox outbox) {
        this.repository = repository;
        this.outbox = outbox;
    }

    @PostMapping("/onboard")
    public ResponseEntity<Onboarded> onboard(@RequestBody OnboardRequest body) {
        if (repository.idempotencyExists(body.idempotencyKey())) {
            String existingId = repository.idempotencyCustomerId(body.idempotencyKey());
            Customer existing = repository.find(existingId).orElseThrow();
            return ResponseEntity.ok(new Onboarded(existing.customerId(), existing.state().name(), true));
        }
        String id = UUID.randomUUID().toString();
        Customer c = new Customer(id, body.fullName(), body.email());
        c.onboard(body.fullName(), body.email());
        repository.save(c);
        repository.markIdempotent(body.idempotencyKey(), id);
        outbox.stage("CustomerOnboarded", id, "{\"customerId\":\"" + id + "\"}");
        return ResponseEntity.accepted().body(new Onboarded(id, c.state().name(), false));
    }

    public record OnboardRequest(String idempotencyKey, String fullName, String email) {}
    public record Onboarded(String customerId, String state, boolean duplicate) {}
}
