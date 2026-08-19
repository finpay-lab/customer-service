package com.finpay.customer.service.domain.port;

import com.finpay.customer.service.domain.event.RetrievedEvent;

import java.util.List;

/**
 * Gateway to the LLM that turns retrieved evidence into a plain-language
 * answer. Interface in domain; the HTTP implementation lives in infrastructure
 * and owns timeout/retry/circuit-breaker plus the BYOK secret.
 *
 * @throws com.finpay.customer.service.domain.exception.LlmUnavailableException
 *         when the provider cannot synthesize an answer
 */
public interface LlmGateway {

    String synthesize(String userQuery, List<RetrievedEvent> evidence);
}