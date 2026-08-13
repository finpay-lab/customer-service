package com.finpay.customer;

import static org.assertj.core.api.Assertions.assertThat;

import com.finpay.common.web.error.ErrorCode;
import org.junit.jupiter.api.Test;

class CustomerServiceApplicationTest {

    @Test
    void application_class_and_common_web_resolve() {
        // Proves the build compiles and com.finpay:common-web is on the classpath
        // via the finpay-platform composite build.
        assertThat(CustomerServiceApplication.class).isNotNull();
        assertThat(ErrorCode.class).isNotNull();
    }
}
