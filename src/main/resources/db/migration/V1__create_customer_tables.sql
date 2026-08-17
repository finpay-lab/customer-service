-- Customer profile aggregate (FP-2). Each service owns its schema (AGENTS.md
-- rule 1); this service owns customer, idempotency and outbox tables.
create table customer (
    id          uuid primary key,
    email       varchar(255) not null unique,
    full_name   varchar(255) not null,
    status      varchar(32)  not null,
    kyc_state   varchar(32)  not null,
    version     bigint       not null default 0,
    created_at  timestamptz  not null,
    updated_at  timestamptz  not null
);

-- Idempotency store (AGENTS.md rule 6): the key is the PK so concurrent
-- same-key inserts are resolved by the DB unique constraint.
create table idempotency_key (
    idempotency_key varchar(128) primary key,
    payload_hash    varchar(64)  not null,
    customer_id     uuid         not null references customer (id),
    created_at      timestamptz  not null
);

-- Transactional outbox (ADR-0004): domain events are appended in the same
-- transaction as the aggregate write and published later by the relay.
create table outbox_event (
    id             uuid         primary key,
    aggregate_type varchar(64)  not null,
    aggregate_id   uuid         not null,
    event_type     varchar(64)  not null,
    payload        jsonb        not null,
    created_at     timestamptz  not null,
    published_at   timestamptz
);

create index idx_outbox_unpublished on outbox_event (published_at, created_at);