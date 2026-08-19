package com.finpay.customer.service.interfaces.web.explain;

import com.finpay.customer.service.domain.event.Explanation;
import com.finpay.customer.service.domain.usecase.ExplainTransaction;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Read-side endpoint: plain-language explanations of a customer's transaction
 * events (RAG over the OpenSearch read model + LLM). No business logic here —
 * maps transport to the {@link ExplainTransaction} use case (Rule 3).
 */
@RestController
@RequestMapping("/explain")
public class ExplainController {

    private final ExplainTransaction explainTransaction;

    public ExplainController(ExplainTransaction explainTransaction) {
        this.explainTransaction = explainTransaction;
    }

    @PostMapping
    public ExplainResponse explain(@Valid @RequestBody ExplainRequest request) {
        Explanation explanation = explainTransaction.explain(request.customerId().toString(), request.query());
        return new ExplainResponse(
                UUID.fromString(explanation.customerId()),
                explanation.query(),
                explanation.answer(),
                explanation.citedEventIds(),
                explanation.evidenceFound());
    }
}