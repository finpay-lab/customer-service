package com.finpay.customer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * FinPay customer-service. Scans the whole {@code com.finpay} base so shared
 * common-web components (correlation id filter) are picked up from the
 * finpay-platform composite build.
 */
@SpringBootApplication(scanBasePackages = "com.finpay")
@EnableScheduling
public class CustomerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }
}
