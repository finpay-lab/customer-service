package com.finpay.customer.service.interfaces.web.explain;

import java.util.List;
import java.util.UUID;

/** POST /explain response body. */
public record ExplainResponse(
        UUID customerId,
        String query,
        String answer,
        List<String> citedEventIds,
        boolean evidenceFound) {
}