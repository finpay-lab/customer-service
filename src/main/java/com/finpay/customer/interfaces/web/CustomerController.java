package com.finpay.customer.interfaces.web;

import com.finpay.customer.application.ApproveKycUseCase;
import com.finpay.customer.application.ChangeCustomerStatusUseCase;
import com.finpay.customer.application.CreateCustomerUseCase;
import com.finpay.customer.application.GetCustomerUseCase;
import com.finpay.customer.application.RevokeKycUseCase;
import com.finpay.customer.interfaces.web.dto.ChangeCustomerStatusRequest;
import com.finpay.customer.interfaces.web.dto.CreateCustomerRequest;
import com.finpay.customer.interfaces.web.dto.CustomerResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Transport layer only: maps HTTP &lt;-&gt; use cases. No business logic
 * (AGENTS.md rule 3).
 */
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CreateCustomerUseCase createCustomer;
    private final GetCustomerUseCase getCustomer;
    private final ApproveKycUseCase approveKyc;
    private final RevokeKycUseCase revokeKyc;
    private final ChangeCustomerStatusUseCase changeStatus;

    public CustomerController(
            CreateCustomerUseCase createCustomer,
            GetCustomerUseCase getCustomer,
            ApproveKycUseCase approveKyc,
            RevokeKycUseCase revokeKyc,
            ChangeCustomerStatusUseCase changeStatus) {
        this.createCustomer = createCustomer;
        this.getCustomer = getCustomer;
        this.approveKyc = approveKyc;
        this.revokeKyc = revokeKyc;
        this.changeStatus = changeStatus;
    }

    /**
     * Idempotent creation keyed by the required {@code Idempotency-Key} header:
     * 201 Created on first call, 200 OK with the original aggregate on replay.
     */
    @PostMapping
    public ResponseEntity<CustomerResponse> create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateCustomerRequest request) {
        CreateCustomerUseCase.CreateCustomerResult result = createCustomer.execute(
                new CreateCustomerUseCase.CreateCustomerCommand(idempotencyKey, request.toProfile()));
        return ResponseEntity
                .status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(CustomerResponse.from(result.customer()));
    }

    @GetMapping("/{customerId}")
    public CustomerResponse get(@PathVariable UUID customerId) {
        return CustomerResponse.from(getCustomer.execute(customerId));
    }

    @PostMapping("/{customerId}/kyc/approve")
    public CustomerResponse approveKyc(@PathVariable UUID customerId) {
        return CustomerResponse.from(approveKyc.execute(customerId));
    }

    @PostMapping("/{customerId}/kyc/revoke")
    public CustomerResponse revokeKyc(@PathVariable UUID customerId) {
        return CustomerResponse.from(revokeKyc.execute(customerId));
    }

    @PatchMapping("/{customerId}/status")
    public CustomerResponse changeStatus(
            @PathVariable UUID customerId,
            @Valid @RequestBody ChangeCustomerStatusRequest request) {
        return CustomerResponse.from(changeStatus.execute(customerId, request.status()));
    }
}
