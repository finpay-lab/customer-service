package com.finpay.customer.application;

import com.finpay.customer.application.port.OutboxAppender;
import com.finpay.customer.domain.event.CustomerCreated;
import com.finpay.customer.domain.event.DomainEvent;
import com.finpay.customer.domain.model.Customer;
import com.finpay.customer.domain.model.CustomerProfile;
import com.finpay.customer.domain.model.CustomerStatus;
import com.finpay.customer.domain.repository.CustomerRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateCustomerUseCaseTest {

    private static final CustomerProfile PROFILE =
            new CustomerProfile("Ada", "Lovelace", "ada@finpay.example", "+1-555-0100", "GB");

    private final InMemoryCustomerRepository repository = new InMemoryCustomerRepository();
    private final RecordingOutbox outbox = new RecordingOutbox();
    private final CreateCustomerUseCase useCase = new CreateCustomerUseCase(repository, outbox);

    @Test
    void first_call_creates_customer_in_kyc_pending_and_enqueues_customer_created() {
        CreateCustomerUseCase.CreateCustomerResult result =
                useCase.execute(new CreateCustomerUseCase.CreateCustomerCommand("key-1", PROFILE));

        assertThat(result.created()).isTrue();
        assertThat(result.customer().status()).isEqualTo(CustomerStatus.KYC_PENDING);
        assertThat(result.customer().isKycVerified()).isFalse();
        assertThat(outbox.events).hasSize(1)
                .singleElement().isInstanceOf(CustomerCreated.class);
    }

    @Test
    void replaying_the_same_key_with_the_same_profile_returns_the_original_without_duplicate_event() {
        CreateCustomerUseCase.CreateCustomerResult first =
                useCase.execute(new CreateCustomerUseCase.CreateCustomerCommand("key-1", PROFILE));

        CreateCustomerUseCase.CreateCustomerResult replay =
                useCase.execute(new CreateCustomerUseCase.CreateCustomerCommand("key-1", PROFILE));

        assertThat(replay.created()).isFalse();
        assertThat(replay.customer().id()).isEqualTo(first.customer().id());
        assertThat(outbox.events).hasSize(1);
    }

    @Test
    void replaying_the_same_key_with_a_different_payload_is_rejected() {
        useCase.execute(new CreateCustomerUseCase.CreateCustomerCommand("key-1", PROFILE));

        CustomerProfile different = new CustomerProfile("Grace", "Hopper", "grace@finpay.example", null, "US");

        assertThatThrownBy(() -> useCase.execute(
                new CreateCustomerUseCase.CreateCustomerCommand("key-1", different)))
                .isInstanceOf(IdempotencyConflictException.class);
        assertThat(outbox.events).hasSize(1);
    }

    @Test
    void different_idempotency_keys_create_distinct_customers() {
        useCase.execute(new CreateCustomerUseCase.CreateCustomerCommand("key-1", PROFILE));
        useCase.execute(new CreateCustomerUseCase.CreateCustomerCommand("key-2", PROFILE));

        assertThat(repository.byKey).hasSize(2);
        assertThat(outbox.events).hasSize(2);
    }

    private static final class InMemoryCustomerRepository implements CustomerRepository {
        private final Map<String, Customer> byKey = new LinkedHashMap<>();
        private final Map<UUID, Customer> byId = new LinkedHashMap<>();

        @Override
        public Customer save(Customer customer) {
            byKey.put(customer.idempotencyKey(), customer);
            byId.put(customer.id(), customer);
            return customer;
        }

        @Override
        public Optional<Customer> findById(UUID customerId) {
            return Optional.ofNullable(byId.get(customerId));
        }

        @Override
        public Optional<Customer> findByIdempotencyKey(String idempotencyKey) {
            return Optional.ofNullable(byKey.get(idempotencyKey));
        }
    }

    private static final class RecordingOutbox implements OutboxAppender {
        private final List<DomainEvent> events = new ArrayList<>();

        @Override
        public void append(DomainEvent event) {
            events.add(event);
        }
    }
}
