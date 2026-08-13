package com.finpay.customer.infrastructure.persistence;

import com.finpay.customer.domain.model.Customer;
import com.finpay.customer.domain.model.CustomerProfile;
import org.springframework.stereotype.Component;

/** Maps between the JPA entity and the domain aggregate. */
@Component
public class CustomerMapper {

    public CustomerEntity toEntity(Customer customer) {
        CustomerEntity entity = new CustomerEntity();
        entity.setId(customer.id());
        entity.setIdempotencyKey(customer.idempotencyKey());
        entity.setFirstName(customer.profile().firstName());
        entity.setLastName(customer.profile().lastName());
        entity.setEmail(customer.profile().email());
        entity.setPhone(customer.profile().phone());
        entity.setCountry(customer.profile().country());
        entity.setStatus(customer.status());
        entity.setKycVerified(customer.isKycVerified());
        entity.setCreatedAt(customer.createdAt());
        entity.setUpdatedAt(customer.updatedAt());
        return entity;
    }

    public Customer toDomain(CustomerEntity entity) {
        return Customer.reconstruct(
                entity.getId(),
                entity.getIdempotencyKey(),
                new CustomerProfile(
                        entity.getFirstName(),
                        entity.getLastName(),
                        entity.getEmail(),
                        entity.getPhone(),
                        entity.getCountry()),
                entity.getStatus(),
                entity.isKycVerified(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
