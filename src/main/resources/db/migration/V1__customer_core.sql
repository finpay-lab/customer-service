-- FP-31/58: customer schema. No shared DB (Rule 1).
CREATE TABLE IF NOT EXISTS customers (
    customer_id VARCHAR(36) PRIMARY KEY,
    full_name   VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    state       VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP     NOT NULL,
    updated_at  TIMESTAMP     NOT NULL
);

-- Rule 6: idempotent onboarding.
CREATE TABLE IF NOT EXISTS customer_onboarding_idempotency (
    idempotency_key VARCHAR(72) PRIMARY KEY,
    customer_id     VARCHAR(36) NOT NULL
);

-- Outbox (Rule 5). Customer events fanned out to finpay.customer topic.
CREATE TABLE IF NOT EXISTS customer_outbox (
    id           VARCHAR(36) PRIMARY KEY,
    event_type   VARCHAR(48) NOT NULL,
    aggregate_id VARCHAR(36) NOT NULL,
    payload      TEXT        NOT NULL,
    created_at   TIMESTAMP   NOT NULL,
    sent         BOOLEAN     NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS ix_customer_outbox_unsent ON customer_outbox (sent, created_at);
