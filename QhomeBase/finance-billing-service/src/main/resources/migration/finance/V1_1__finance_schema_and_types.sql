CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE SCHEMA IF NOT EXISTS finance;

DO $do$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_type t
    JOIN pg_namespace n ON n.oid = t.typnamespace
    WHERE t.typname = 'pay_status' AND n.nspname = 'finance'
  ) THEN
CREATE TYPE finance.pay_status AS ENUM ('PENDING','SUCCEEDED','FAILED');
END IF;
END
$do$;

DO $do$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_type t
    JOIN pg_namespace n ON n.oid = t.typnamespace
    WHERE t.typname = 'allocation_type' AND n.nspname = 'finance'
  ) THEN
CREATE TYPE finance.allocation_type AS ENUM ('INVOICE','INVOICE_LINE');
END IF;
END
$do$;

DO $do$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_type t
    JOIN pg_namespace n ON n.oid = t.typnamespace
    WHERE t.typname = 'intent_target_type' AND n.nspname = 'finance'
  ) THEN
CREATE TYPE finance.intent_target_type AS ENUM ('INVOICE','INVOICE_LINE');
END IF;
END
$do$;

DO $do$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_type t
    JOIN pg_namespace n ON n.oid = t.typnamespace
    WHERE t.typname = 'payment_method' AND n.nspname = 'finance'
  ) THEN
CREATE TYPE finance.payment_method AS ENUM ('CASH','BANK_TRANSFER','MOMO');
END IF;
END
$do$;

CREATE TABLE IF NOT EXISTS finance.payment_gateways (
                                                        id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    code      TEXT NOT NULL,
    name      TEXT NOT NULL,
    CONSTRAINT uq_gateway_code_per_tenant UNIQUE (tenant_id, code)
    );

CREATE TABLE IF NOT EXISTS finance.payments (
                                                id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL,
    receipt_no        TEXT NOT NULL,
    method            finance.payment_method NOT NULL,
    cash_account_id   UUID,
    paid_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    amount_total      NUMERIC(14,4) NOT NULL CHECK (amount_total >= 0),
    currency          TEXT NOT NULL DEFAULT 'VND',
    status            finance.pay_status NOT NULL DEFAULT 'SUCCEEDED',
    note              TEXT,
    payer_resident_id UUID,
    CONSTRAINT uq_payment_receipt_per_tenant UNIQUE (tenant_id, receipt_no)
    );

CREATE TABLE IF NOT EXISTS finance.payment_allocations (
                                                           id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID NOT NULL,
    payment_id       UUID NOT NULL,
    allocation_type  finance.allocation_type NOT NULL,
    invoice_id       UUID,
    invoice_line_id  UUID,
    amount           NUMERIC(14,4) NOT NULL CHECK (amount >= 0),
    CONSTRAINT fk_alloc_payment
    FOREIGN KEY (payment_id)      REFERENCES finance.payments(id)       ON DELETE CASCADE,
    CONSTRAINT fk_alloc_invoice
    FOREIGN KEY (invoice_id)      REFERENCES billing.invoices(id)       ON DELETE CASCADE,
    CONSTRAINT fk_alloc_invoice_line
    FOREIGN KEY (invoice_line_id) REFERENCES billing.invoice_lines(id)  ON DELETE CASCADE,
    CONSTRAINT ck_alloc_target CHECK (
(allocation_type = 'INVOICE'      AND invoice_id IS NOT NULL     AND invoice_line_id IS NULL) OR
(allocation_type = 'INVOICE_LINE' AND invoice_line_id IS NOT NULL AND invoice_id IS NULL)
    )
    );

CREATE TABLE IF NOT EXISTS finance.gateway_transactions (
                                                            id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL,
    gateway_id  UUID NOT NULL,
    ext_txn_id  TEXT NOT NULL,
    status      TEXT NOT NULL,
    amount      NUMERIC(14,4) NOT NULL CHECK (amount >= 0),
    currency    TEXT NOT NULL DEFAULT 'VND',
    occurred_at TIMESTAMPTZ NOT NULL,
    raw_payload JSONB,
    CONSTRAINT uq_gateway_ext_txn UNIQUE (tenant_id, gateway_id, ext_txn_id),
    CONSTRAINT fk_gtw_txn_gateway
    FOREIGN KEY (gateway_id) REFERENCES finance.payment_gateways(id) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS finance.gateway_payment_links (
                                                             id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID NOT NULL,
    gateway_txn_id UUID NOT NULL,
    payment_id     UUID NOT NULL,
    CONSTRAINT uq_gpl_txn_per_tenant     UNIQUE (tenant_id, gateway_txn_id),
    CONSTRAINT uq_gpl_payment_per_tenant UNIQUE (tenant_id, payment_id),
    CONSTRAINT fk_gpl_txn
    FOREIGN KEY (gateway_txn_id) REFERENCES finance.gateway_transactions(id) ON DELETE CASCADE,
    CONSTRAINT fk_gpl_payment
    FOREIGN KEY (payment_id)     REFERENCES finance.payments(id)            ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS finance.payment_intents (
                                                       id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID NOT NULL,
    intent_key       TEXT NOT NULL,
    currency         TEXT NOT NULL DEFAULT 'VND',
    amount_total     NUMERIC(14,4) NOT NULL CHECK (amount_total >= 0),
    status           TEXT NOT NULL DEFAULT 'CREATED',
    payer_resident_id UUID,
    description      TEXT,
    return_url       TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_intent_key_per_tenant UNIQUE (tenant_id, intent_key)
    );

CREATE TABLE IF NOT EXISTS finance.payment_intent_targets (
                                                              id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    intent_id       UUID NOT NULL,
    target_type     finance.intent_target_type NOT NULL,
    invoice_id      UUID,
    invoice_line_id UUID,
    amount_planned  NUMERIC(14,4) NOT NULL CHECK (amount_planned >= 0),
    note            TEXT,
    CONSTRAINT fk_pit_intent
    FOREIGN KEY (intent_id)      REFERENCES finance.payment_intents(id)  ON DELETE CASCADE,
    CONSTRAINT fk_pit_invoice
    FOREIGN KEY (invoice_id)     REFERENCES billing.invoices(id)         ON DELETE CASCADE,
    CONSTRAINT fk_pit_invoice_line
    FOREIGN KEY (invoice_line_id)REFERENCES billing.invoice_lines(id)    ON DELETE CASCADE,
    CONSTRAINT ck_pit_target CHECK (
(target_type = 'INVOICE' AND invoice_id IS NOT NULL AND invoice_line_id IS NULL) OR
(target_type = 'INVOICE_LINE' AND invoice_line_id IS NOT NULL AND invoice_id IS NULL)
    )
    );

CREATE TABLE IF NOT EXISTS finance.payment_attempts (
                                                        id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    intent_id       UUID NOT NULL,
    gateway_id      UUID NOT NULL,
    method          finance.payment_method NOT NULL,
    status          TEXT NOT NULL DEFAULT 'INITIATED',
    amount_expected NUMERIC(14,4) NOT NULL CHECK (amount_expected >= 0),
    currency        TEXT NOT NULL DEFAULT 'VND',
    ext_txn_id      TEXT,
    ext_order_id    TEXT,
    pay_url         TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_attempt_intent
    FOREIGN KEY (intent_id)  REFERENCES finance.payment_intents(id) ON DELETE CASCADE,
    CONSTRAINT fk_attempt_gateway
    FOREIGN KEY (gateway_id) REFERENCES finance.payment_gateways(id) ON DELETE RESTRICT
    );

CREATE TABLE IF NOT EXISTS finance.gateway_webhook_events (
                                                              id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID NOT NULL,
    gateway_id   UUID NOT NULL,
    received_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    signature    TEXT,
    payload      JSONB NOT NULL,
    processed    BOOLEAN NOT NULL DEFAULT FALSE,
    processed_at TIMESTAMPTZ,
    CONSTRAINT fk_webhook_gateway
    FOREIGN KEY (gateway_id) REFERENCES finance.payment_gateways(id) ON DELETE RESTRICT
    );

CREATE TABLE IF NOT EXISTS finance.idempotency_keys (
                                                        id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL,
    idem_key          TEXT NOT NULL,
    first_seen_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    response_snapshot JSONB,
    CONSTRAINT uq_idem_key_per_tenant UNIQUE (tenant_id, idem_key)
    );

CREATE INDEX IF NOT EXISTS idx_payments_tenant_date ON finance.payments (tenant_id, paid_at DESC);
CREATE INDEX IF NOT EXISTS idx_allocations_payment ON finance.payment_allocations (payment_id);
CREATE INDEX IF NOT EXISTS idx_gateway_txns_gateway ON finance.gateway_transactions (gateway_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_payment_attempts_intent ON finance.payment_attempts (intent_id, created_at DESC);
