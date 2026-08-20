package com.finpay.customer.service.web;

import com.finpay.customer.service.domain.TransactionExplainer;
import org.springframework.web.bind.annotation.*;

/** RAG transaction explainer endpoint (FP-58). Delegates to the use case. */
@RestController
@RequestMapping("/v1/explain")
public class ExplainController {

    private final TransactionExplainer explainer;

    public ExplainController(TransactionExplainer explainer) {
        this.explainer = explainer;
    }

    @PostMapping
    public ExplainResponse explain(@RequestBody ExplainRequest body) {
        TransactionExplainer.Answer a = explainer.explain(body.customerId(), body.query());
        return new ExplainResponse(a.text(), a.citedEventIds());
    }

    public record ExplainRequest(String customerId, String query) {}
    public record ExplainResponse(String answer, java.util.List<String> citedEventIds) {}
}
