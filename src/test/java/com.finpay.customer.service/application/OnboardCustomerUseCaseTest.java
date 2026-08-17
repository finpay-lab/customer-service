package com.finpay.customer.service.application;

import com.finpay.customer.service.domain.event.CustomerCreatedEvent;
import com.finpay.customer.service.domain.exception.CustomerAlreadyExistsException;
import com.finpay.customer.service.domain.exception.IdempotencyConflictException;
import com.finpay.customer.service.domain.model.CustomerStatus;
import com.finpay.customer.service.domain.model.KycState;
import com.finpay.customer.service.testutil.FakeCustomerRepository;
import com.finpay.customer.service.testutil.FakeIdempotencyRecordRepository;
import com.finpay.customer.service.testutil.FakeOutboxRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OnboardCustomerUseCaseTest {

    private final FakeCustomerRepository customers = new FakeCustomerRepository();
    private final FakeIdempotencyRecordRepository idempotency = new FakeIdempotencyRecordRepository();
    private final FakeOutboxRepository outbox = new FakeOutboxRepository();
    private final OnboardCustomerUseCase useCase = new OnboardCustomerUseCase(customers, idempotency, outbox);

    @Test
    void onboard_creates_customer_and_persists_idempotency_and_outbox_in_flow() {
        OnboardCustomerResult result = useCase.onboard(
                new OnboardCustomerCommand("key-1", "ada@example.com", "Ada Lovelace"));

        assertThat(result.created()).isTrue();
        assertThat(result.status()).isEqualTo(CustomerStatus.PENDING.name());
        assertThat(result.kycState()).isEqualTo(KycState.NOT_STARTED.name());
        assertThat(customers.size()).isEqualTo(1);
        assertThat(idempotency.findByIdempotencyKey("key-1")).isPresent();
        assertThat(outbox.events()).hasSize(1);
        assertThat(outbox.events().get(0)).isInstanceOf(CustomerCreatedEvent.class);
    }

    @Test
    void replay_with_same_key_and_payload_returns_original_without_duplicate() {
        OnboardCustomerResult first = useCase.onboard(
                new OnboardCustomerCommand("key-1", "ada@example.com", "Ada Lovelace"));

        OnboardCustomerResult replay = useCase.onboard(
                new OnboardCustomerCommand("key-1", "ada@example.com", "Ada Lovelace"));

        assertThat(replay.created()).isFalse();
        assertThat(replay.customerId()).isEqualTo(first.customerId());
        assertThat(customers.size()).isEqualTo(1);
        assertThat(outbox.events()).hasSize(1);
    }

    @Test
    void replay_with_same_key_but_different_payload_is_rejected_as_conflict() {
        useCase.onboard(new OnboardCustomerCommand("key-1", "ada@example.com", "Ada Lovelace"));

        assertThatThrownBy(() -> useCase.onboard(
                new OnboardCustomerCommand("key-1", "ada@example.com", "Ada Lovelace II")))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void same_email_with_different_key_is_rejected_as_duplicate() {
        useCase.onboard(new OnboardCustomerCommand("key-1", "ada@example.com", "Ada Lovelace"));

        assertThatThrownBy(() -> useCase.onboard(
                new OnboardCustomerCommand("key-2", "ada@example.com", "Ada Lovelace")))
                .isInstanceOf(CustomerAlreadyExistsException.class);
    }

    @Test
    void email_is_normalized_before_hashing() {
        useCase.onboard(new OnboardCustomerCommand("key-1", " Ada@Example.COM ", "  Ada  "));
        assertThat(useCase.onboard(new OnboardCustomerCommand("key-1", "ada@example.com", "Ada")))
                .satisfies(r -> assertThat(r.created()).isFalse());
    }
}