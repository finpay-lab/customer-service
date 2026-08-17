package com.finpay.customer.service.interfaces.web;

import com.finpay.customer.service.application.ChangeCustomerStatusCommand;
import com.finpay.customer.service.application.ChangeCustomerStatusUseCase;
import com.finpay.customer.service.application.ChangeKycStateCommand;
import com.finpay.customer.service.application.ChangeKycStateUseCase;
import com.finpay.customer.service.application.OnboardCustomerCommand;
import com.finpay.customer.service.application.OnboardCustomerResult;
import com.finpay.customer.service.application.OnboardCustomerUseCase;
import com.finpay.customer.service.domain.exception.CustomerNotFoundException;
import com.finpay.customer.service.domain.model.Customer;
import com.finpay.customer.service.domain.repository.CustomerRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Transport ↔ use-case mapping only — no business logic (AGENTS.md rule 3).
 */
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final OnboardCustomerUseCase onboardUseCase;
    private final ChangeCustomerStatusUseCase changeStatusUseCase;
    private final ChangeKycStateUseCase changeKycUseCase;
    private final CustomerRepository customerRepository;

    public CustomerController(OnboardCustomerUseCase onboardUseCase,
                              ChangeCustomerStatusUseCase changeStatusUseCase,
                              ChangeKycStateUseCase changeKycUseCase,
                              CustomerRepository customerRepository) {
        this.onboardUseCase = onboardUseCase;
        this.changeStatusUseCase = changeStatusUseCase;
        this.changeKycUseCase = changeKycUseCase;
        this.customerRepository = customerRepository;
    }

    /** Idempotent onboarding — replaying the same key + payload returns the original customer. */
    @PostMapping("/onboard")
    public ResponseEntity<OnboardCustomerResponse> onboard(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody OnboardCustomerRequest request) {
        OnboardCustomerResult result = onboardUseCase.onboard(
                new OnboardCustomerCommand(idempotencyKey, request.email(), request.fullName()));
        return ResponseEntity
                .status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(toResponse(result));
    }

    @GetMapping("/{customerId}")
    public OnboardCustomerResponse get(@PathVariable UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer " + customerId + " does not exist"));
        return new OnboardCustomerResponse(customer.id(), customer.status().name(),
                customer.kycState().name(), customer.createdAt());
    }

    @PostMapping("/{customerId}/status")
    public ChangeCustomerStatusResponse changeStatus(
            @PathVariable UUID customerId,
            @Valid @RequestBody ChangeCustomerStatusRequest request) {
        String status = changeStatusUseCase.change(
                new ChangeCustomerStatusCommand(customerId, request.action())).name();
        return new ChangeCustomerStatusResponse(customerId, status);
    }

    @PostMapping("/{customerId}/kyc")
    public ChangeKycStateResponse changeKyc(
            @PathVariable UUID customerId,
            @Valid @RequestBody ChangeKycStateRequest request) {
        String kycState = changeKycUseCase.change(
                new ChangeKycStateCommand(customerId, request.action())).name();
        return new ChangeKycStateResponse(customerId, kycState);
    }

    private OnboardCustomerResponse toResponse(OnboardCustomerResult result) {
        return new OnboardCustomerResponse(result.customerId(), result.status(), result.kycState(), result.createdAt());
    }

    public record ChangeCustomerStatusResponse(UUID customerId, String status) {
    }

    public record ChangeKycStateResponse(UUID customerId, String kycState) {
    }
}