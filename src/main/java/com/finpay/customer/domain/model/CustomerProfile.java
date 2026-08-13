package com.finpay.customer.domain.model;

/**
 * Customer profile (identity/contact attributes). Value object, immutable.
 */
public record CustomerProfile(
        String firstName,
        String lastName,
        String email,
        String phone,
        String country
) {
}
