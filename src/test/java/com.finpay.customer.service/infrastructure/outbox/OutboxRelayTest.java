package com.finpay.customer.service.infrastructure.outbox;

import com.finpay.customer.service.domain.event.CustomerCreatedEvent;
import com.finpay.customer.service.domain.model.Customer;
import com.finpay.customer.service.domain.repository.OutboxRepository;
import com.finpay.customer.service.testutil.FakeEventPublisher;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxRelayTest {

    private static final String TOPIC = "finpay.customer";

    @Test
    void publishes_pending_rows_and_marks_them_published() {
        FakeOutbox outbox = new FakeOutbox();
        outbox.save(CustomerCreatedEvent.of(Customer.onboard("ada@example.com", "Ada Lovelace")));
        outbox.save(CustomerCreatedEvent.of(Customer.onboard("grace@example.com", "Grace Hopper")));
        FakeEventPublisher publisher = new FakeEventPublisher();

        new OutboxRelay(outbox, publisher, TOPIC).publishPending();

        assertThat(publisher.published()).hasSize(2);
        assertThat(outbox.publishedCount()).isEqualTo(2);
        assertThat(publisher.published().get(0).topic()).isEqualTo(TOPIC);
        assertThat(publisher.published().get(0).partitionKey())
                .matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    }

    @Test
    void failed_publish_is_not_marked_and_relay_retries_next_tick() {
        FakeOutbox outbox = new FakeOutbox();
        outbox.save(CustomerCreatedEvent.of(Customer.onboard("ada@example.com", "Ada Lovelace")));
        FakeEventPublisher publisher = new FakeEventPublisher().fail(true);

        new OutboxRelay(outbox, publisher, TOPIC).publishPending();

        assertThat(publisher.published()).isEmpty();
        assertThat(outbox.publishedCount()).isZero();

        publisher.fail(false);
        new OutboxRelay(outbox, publisher, TOPIC).publishPending();
        assertThat(publisher.published()).hasSize(1);
        assertThat(outbox.publishedCount()).isEqualTo(1);
    }

    /** Outbox fake tracking markPublished. */
    private static final class FakeOutbox implements OutboxRepository {
        private final List<OutboxRecord> records = new ArrayList<>();
        private final List<UUID> marked = new ArrayList<>();

        @Override
        public void save(com.finpay.customer.service.domain.event.DomainEvent event) {
            records.add(new OutboxRecord(UUID.randomUUID(), "CUSTOMER", UUID.randomUUID(),
                    event.eventType(), "{}"));
        }

        @Override
        public List<OutboxRecord> findUnpublished(int limit) {
            return List.copyOf(records);
        }

        @Override
        public void markPublished(UUID outboxId) {
            marked.add(outboxId);
        }

        int publishedCount() {
            return marked.size();
        }
    }
}