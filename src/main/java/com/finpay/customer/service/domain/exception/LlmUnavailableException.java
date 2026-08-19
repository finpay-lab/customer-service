package com.finpay.customer.service.domain.exception;

/**
 * Raised when the LLM provider cannot synthesize an answer (circuit open,
 * retries exhausted, bad response). The use case falls back to a deterministic
 * answer that still cites the retrieved event ids.
 */
public class LlmUnavailableException extends RuntimeException {

    public LlmUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}