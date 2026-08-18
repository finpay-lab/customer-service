package com.finpay.customer.service.application;

import com.finpay.customer.service.domain.event.CustomerCreatedEvent;
import com.finpay.customer.service.domain.exception.CustomerAlreadyExistsException;
import com.finpay.customer.service.domain.exception.CustomerNotFoundException;
import com.finpay.customer.service.domain.exception.IdempotencyConflictException;
import com.finpay.customer.service.domain.model.Customer;
import com.finpay.customer.service.domain.model.IdempotencyRecord;
import com.finpay.customer.service.domain.repository.CustomerRepository;
import com.finpay.customer.service.domain.repository.IdempotencyRecordRepository;
import com.finpay.customer.service.domain.repository.OutboxRepository;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Idempotent onboarding (AGENTS.md rule 6). The customer aggregate, the
 * idempotency record and the CustomerCreated outbox row are written in one
 * transaction — the outbox is published later by the relay, outside the
 * transaction (rule 5). Replaying the same key + payload returns the original
 * customer; a key replayed with a different payload is rejected as a conflict.
 */
@org.springframework.stereotype.Service
public class OnboardCustomerUseCase {

    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 128;

    private final CustomerRepository customerRepository;
    private final IdempotencyRecordRepository idempotencyRepository;
    private final OutboxRepository outboxRepository;

    public OnboardCustomerUseCase(CustomerRepository customerRepository,
                                  IdempotencyRecordRepository idempotencyRepository,
                                  OutboxRepository outboxRepository) {
        this.customerRepository = customerRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public OnboardCustomerResult onboard(OnboardCustomerCommand command) {
        validateKey(command.idempotencyKey());
        String payloadHash = payloadHash(command.email(), command.fullName());

        Optional<IdempotencyRecord> existing = idempotencyRepository.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            return existing.get().payloadHash().equals(payloadHash)
                    ? toResult(existing.get(), payloadHash)
                    : throwConflict(command.idempotencyKey());
        }

        Customer customer = Customer.onboard(command.email(), command.fullName());
        try {
            customerRepository.save(customer);
        } catch (CustomerAlreadyExistsException e) {
            // Unique-email violation: another request may have won the race with
            // the same idempotency key + payload. Re-check the key before failing.
            Optional<IdempotencyRecord> raced = idempotencyRepository.findByIdempotencyKey(command.idempotencyKey());
            if (raced.isPresent() && raced.get().payloadHash().equals(payloadHash)) {
                return toResult(raced.get(), payloadHash);
            }
            throw e;
        }

        boolean inserted = idempotencyRepository.insertIfAbsent(
                new IdempotencyRecord(command.idempotencyKey(), payloadHash, customer.id(), Instant.now()));
        if (!inserted) {
            // Concurrent request with the same key committed first.
            IdempotencyRecord raced = idempotencyRepository.findByIdempotencyKey(command.idempotencyKey())
                    .orElseThrow(() -> new IllegalStateException("Idempotency key disappeared after insert"));
            if (!raced.payloadHash().equals(payloadHash)) {
                throwConflict(command.idempotencyKey());
            }
            return toResult(raced, payloadHash);
        }

        outboxRepository.save(CustomerCreatedEvent.of(customer));
        return new OnboardCustomerResult(customer.id(), customer.status().name(), customer.kycState().name(),
                customer.createdAt(), true);
    }

    private OnboardCustomerResult toResult(IdempotencyRecord record, String expectedHash) {
        Customer customer = customerRepository.findById(record.customerId())
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Idempotency record references missing customer " + record.customerId()));
        return new OnboardCustomerResult(customer.id(), customer.status().name(), customer.kycState().name(),
                customer.createdAt(), false);
    }

    private OnboardCustomerResult throwConflict(String key) {
        throw new IdempotencyConflictException(key);
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank() || key.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new IllegalArgumentException("idempotencyKey must be present and <= "
                    + MAX_IDEMPOTENCY_KEY_LENGTH + " characters");
        }
    }

    private String payloadHash(String email, String fullName) {
        String normalized = (email == null ? "" : email.trim().toLowerCase())
                + "\u0000" + (fullName == null ? "" : fullName.trim());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
