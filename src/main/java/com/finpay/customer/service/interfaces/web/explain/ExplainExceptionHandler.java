package com.finpay.customer.service.interfaces.web.explain;

import com.finpay.customer.service.domain.exception.ExplainUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps explainer failures to RFC-9457 problem responses. Validation errors are
 * handled by Spring MVC (spring.mvc.problemdetails.enabled=true); this advice
 * covers the search read-model outage without leaking internals.
 */
@RestControllerAdvice(assignableTypes = ExplainController.class)
public class ExplainExceptionHandler {

    @ExceptionHandler(ExplainUnavailableException.class)
    public ProblemDetail handleSearchUnavailable(ExplainUnavailableException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "The transaction search index is temporarily unavailable. Please retry later.");
        problem.setTitle("Search unavailable");
        problem.setProperty("code", "SEARCH_UNAVAILABLE");
        return problem;
    }
}