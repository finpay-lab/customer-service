package com.finpay.customer.service.domain.event;

/**
 * A single event retrieved from the search index as evidence for an answer.
 *
 * @param eventId     cited identifier (equals the OpenSearch document id)
 * @param eventType   lifecycle event type
 * @param description human-readable rendering passed to the LLM as context
 * @param score       retrieval relevance score (informational)
 */
public record RetrievedEvent(String eventId, EventType eventType, String description, float score) {
}