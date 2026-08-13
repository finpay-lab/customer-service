CREATE TABLE customers (
    id              UUID        PRIMARY KEY,
    idempotency_key VARCHAR(64) NOT NULL,
    first_name      VARCHAR(64) NOT NULL,
    last_name       VARCHAR(64) NOT NULL,
    email           VARCHAR(254) NOT NULL,
    phone           VARCHAR(32),
    country         CHAR(2)     NOT NULL,
    status          VARCHAR(20) NOT NULL,
    kyc_verified    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    version         BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uk_customers_idempotency_key UNIQUE (idempotency_key)
);

-- Customer lifecycle statuses: KYC_PENDING, ACTIVE, FROZEN, CLOSED.
-- Legal transitions are enforced in the domain (Customer state machine).
CREATE INDEX idx_customers_status ON customers (status);

CREATE TABLE outbox_events (
    id            UUID        PRIMARY KEY,
    aggregate_id  UUID        NOT NULL,
    event_type    VARCHAR(64) NOT NULL,
    partition_key VARCHAR(64) NOT NULL,
    payload       TEXT        NOT NULL,
    published     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at  TIMESTAMPTZ,
    CONSTRAINT uk_outbox_events_event_id UNIQUE (id)
);

-- Relay polls unpublished rows in insertion order (ADR-0004).
CREATE INDEX idx_outbox_events_unpublished
    ON outbox_events (created_at)
    WHERE published = FALSE;
