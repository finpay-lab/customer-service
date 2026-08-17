package com.finpay.customer.service;

import com.finpay.common.test.ArchitectureRules;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

/** Enforces the shared FinPay architecture rules (AGENTS.md rule 4). */
class ArchitectureTest {

    @Test
    void domain_is_independent_of_infrastructure() {
        ArchitectureRules.domainIsIndependentOfInfrastructure()
                .check(new ClassFileImporter().importPackages("com.finpay.customer.service"));
    }
}