package com.finpay.customer.service.testutil;

import com.finpay.customer.service.application.port.EventPublisher;

import java.util.ArrayList;
import java.util.List;

/** Records publishes for assertions; can be programmed to fail. */
public class FakeEventPublisher implements EventPublisher {

    public record Published(String topic, String partitionKey, String payloadJson) {
    }

    private final List<Published> published = new ArrayList<>();
    private volatile boolean failing;

    public FakeEventPublisher fail(boolean failing) {
        this.failing = failing;
        return this;
    }

    @Override
    public void publish(String topic, String partitionKey, String payloadJson) {
        if (failing) {
            throw new IllegalStateException("broker unavailable");
        }
        published.add(new Published(topic, partitionKey, payloadJson));
    }

    public List<Published> published() {
        return published;
    }
}