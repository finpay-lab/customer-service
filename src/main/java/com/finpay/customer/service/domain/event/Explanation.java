package com.finpay.customer.service.domain.event;

import java.util.List;

/**
 * Result of an explain request: a plain-language answer plus the event ids the
 * answer is grounded on.
 *
 * @param customerId    customer the question was asked about
 * @param query         original question
 * @param answer        synthesized (or fallback) answer
 * @param citedEventIds eventIds the answer cites as evidence
 * @param evidenceFound whether any indexed events were retrieved
 */
public record Explanation(
        String customerId,
        String query,
        String answer,
        List<String> citedEventIds,
        boolean evidenceFound) {
}