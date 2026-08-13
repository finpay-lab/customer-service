## Summary

Implements customer-service (FP-2): the Customer aggregate with profile, a
status state machine (ACTIVE / FROZEN / KYC_PENDING / CLOSED, legal transitions
only), and a KYC verification flag. Owns its own PostgreSQL schema via Flyway
(ADR-0005), publishes `CustomerCreated` / `CustomerKycChanged` through a
transactional outbox (ADR-0004) to the `finpay.customer` topic, and enforces
idempotent creation (Rule 6). Consumes `com.finpay:*` shared libs via the
finpay-platform composite build; domain logic is free of Spring/JPA/Kafka
imports (Rule 4).

## Changes

- **Domain** (`com.finpay.customer.domain`): `Customer` aggregate (profile,
  status state machine, `kycVerified` flag, recorded domain events),
  `CustomerStatus` enum, `CustomerProfile`, `IllegalStateTransitionException`,
  `CustomerCreated` / `CustomerKycChanged` events (sealed `DomainEvent`), and
  the `CustomerRepository` interface. No framework imports.
- **Application**: `CreateCustomerUseCase` (idempotent by `Idempotency-Key`,
  conflict → `IDEMPOTENCY_CONFLICT`), `GetCustomerUseCase`,
  `ApproveKycUseCase`, `RevokeKycUseCase`, `ChangeCustomerStatusUseCase`
  (freeze/unfreeze/close only; KYC_PENDING is reachable only via the KYC
  endpoints), plus `OutboxAppender` port. `@Transactional` persists + enqueues
  outbox rows atomically; no remote calls inside the transaction.
- **Interfaces/web**: `CustomerController` (thin transport mapping),
  `CreateCustomerRequest` / `ChangeCustomerStatusRequest` / `CustomerResponse`
  DTOs, `ApiExceptionHandler` mapping to common-web RFC-9457 `ProblemDetail`
  with correlation id.
- **Infrastructure**: JPA entities + Spring Data repos, `JpaCustomerRepository`
  / `JpaOutboxAppender` adapters, `DomainEventSerializer` (v1 event envelope),
  `OutboxRelay` (`@Scheduled` poll → publish → mark published, retries on
  failure), `KafkaDomainEventPublisher` (blocking send, at-least-once). JPA
  `@Version` optimistic locking on the customer row.
- **Schema/config**: `db/migration/V1__create_customer_schema.sql` (customers +
  outbox_events + unpublished index), `application.yml` (datasource, Flyway
  `ddl-auto: none`, Kafka, topic, outbox poll interval), `build.gradle` adds
  `java-conventions`, JPA/Flyway/Postgres/Kafka deps and `common-test`.
- **Tests**: state-machine legal/illegal transitions, idempotent create
  (replay returns original without duplicate event; payload mismatch rejected),
  controller delegation, exception → problem mapping, and the shared ArchUnit
  domain-independence rule.

## Testing

- `docker run --rm -v "$PWD":/work -w /work -v gradle-cache:/root/.gradle gradle:8.10.2-jdk21 gradle clean build -Pversion=0.0.1 --no-daemon` → **BUILD SUCCESSFUL** (all 22 tests pass, including `DomainArchitectureTest`).
- Verified manually: serialized outbox payload for `CustomerCreated` matches
  `contracts/events/v1/CustomerCreated.json` envelope/`kycState` mapping.

## Risks

- The issue's status model (ACTIVE/FROZEN/KYC_PENDING/CLOSED, boolean KYC flag,
  `CustomerKycChanged`) intentionally differs from the older platform contract
  draft (PENDING/ACTIVE/SUSPENDED/BLOCKED/CLOSED, `KycStateChanged`); consumers
  should key off the issue spec. Contracts not touched (submodule).
- `KafkaDomainEventPublisher` blocks up to 10s per send; a broker outage makes
  the relay back off, but business writes are unaffected (backlog drains on
  recovery). Poison-message/DLQ handling is out of scope (FP-15).
- Outbox payload stored as TEXT rather than JSONB (never queried by the relay).
- No `@SpringBootTest` / Testcontainers integration test yet — the app context
  is exercised at runtime, not in CI (no broker/DB in the build container).
