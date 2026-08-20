package com.finpay.customer.service.infrastructure.config;

import com.finpay.customer.service.domain.CustomerOutbox;
import com.finpay.customer.service.domain.CustomerRepository;
import com.finpay.customer.service.domain.TransactionExplainer;
import com.finpay.customer.service.infrastructure.explainer.HttpLlmSynthesizer;
import com.finpay.customer.service.infrastructure.explainer.OpenSearchEventStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomerConfig {

    @Bean
    public TransactionExplainer transactionExplainer(CustomerRepository repository,
                                                      CustomerOutbox outbox,
                                                      OpenSearchEventStore store,
                                                      HttpLlmSynthesizer llm) {
        return new TransactionExplainer(store, llm);
    }
}
