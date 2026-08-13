package com.finpay.customer.architecture;

import com.finpay.common.test.ArchitectureRules;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Enforces AGENTS.md rule 4: domain logic must not depend on Spring/JPA/Kafka
 * (shared rule from finpay-platform common-test).
 */
@AnalyzeClasses(packages = "com.finpay.customer.domain")
class DomainArchitectureTest {

    @ArchTest
    static final ArchRule domain_is_independent_of_infrastructure =
            ArchitectureRules.domainIsIndependentOfInfrastructure();
}
