package com.finpay.customer.service.infrastructure.persistence;

import com.finpay.customer.service.domain.exception.CustomerAlreadyExistsException;
import com.finpay.customer.service.domain.model.Customer;
import com.finpay.customer.service.domain.model.CustomerStatus;
import com.finpay.customer.service.domain.model.KycState;
import com.finpay.customer.service.domain.repository.CustomerRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA-backed implementation of the domain {@link CustomerRepository}. Unique
 * business invariants (email) surface from the DB constraint as a domain
 * exception so the application layer stays Spring-free where it matters.
 */
@Repository
public class CustomerJpaRepository implements CustomerRepository {

    private final CustomerSpringRepository springRepository;

    public CustomerJpaRepository(CustomerSpringRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    public void save(Customer customer) {
        try {
            springRepository.saveAndFlush(toEntity(customer));
        } catch (DataIntegrityViolationException e) {
            throw new CustomerAlreadyExistsException("A customer with email '" + customer.email() + "' already exists");
        }
    }

    @Override
    public Optional<Customer> findById(UUID customerId) {
        return springRepository.findById(customerId).map(CustomerJpaRepository::toDomain);
    }

    static Customer toDomain(CustomerJpaEntity e) {
        return Customer.restore(
                e.getId(), e.getEmail(), e.getFullName(),
                e.getStatus(), e.getKycState(),
                e.getCreatedAt(), e.getUpdatedAt(), e.getVersion());
    }

    static CustomerJpaEntity toEntity(Customer c) {
        CustomerJpaEntity e = new CustomerJpaEntity();
        e.setId(c.id());
        e.setEmail(c.email());
        e.setFullName(c.fullName());
        e.setStatus(c.status() == null ? CustomerStatus.PENDING : c.status());
        e.setKycState(c.kycState() == null ? KycState.NOT_STARTED : c.kycState());
        e.setVersion(c.version());
        e.setCreatedAt(c.createdAt());
        e.setUpdatedAt(c.updatedAt());
        return e;
    }
}