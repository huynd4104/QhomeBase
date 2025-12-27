CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE SCHEMA IF NOT EXISTS billing;

DO $do$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_type t
    JOIN pg_namespace n ON n.oid = t.typnamespace
    WHERE t.typname = 'inv_status' AND n.nspname = 'billing'
  ) THEN
CREATE TYPE billing.inv_status AS ENUM ('DRAFT','PUBLISHED','PAID','VOID');
END IF;
END
$do$;

CREATE TABLE IF NOT EXISTS billing.billing_cycles (
                                                      id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID NOT NULL,
    name         TEXT NOT NULL,
    period_from  DATE NOT NULL,
    period_to    DATE NOT NULL,
    status       TEXT NOT NULL DEFAULT 'OPEN',
    CONSTRAINT uq_billing_cycles UNIQUE (tenant_id, name, period_from, period_to),
    CONSTRAINT ck_period_range CHECK (period_from <= period_to)
    );

CREATE TABLE IF NOT EXISTS billing.invoices (
                                                id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL,
    code              TEXT NOT NULL,
    issued_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    due_date          DATE,
    status            billing.inv_status NOT NULL DEFAULT 'DRAFT',
    currency          TEXT NOT NULL DEFAULT 'VND',
    bill_to_name      TEXT,
    bill_to_address   TEXT,
    bill_to_contact   TEXT,
    payer_unit_id     UUID,
    payer_resident_id UUID,
    cycle_id          UUID,
    CONSTRAINT uq_invoices_code_per_tenant UNIQUE (tenant_id, code),
    CONSTRAINT fk_invoice_cycle
    FOREIGN KEY (cycle_id) REFERENCES billing.billing_cycles(id) ON DELETE SET NULL
    );

CREATE TABLE IF NOT EXISTS billing.invoice_lines (
                                                     id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id          UUID NOT NULL,
    invoice_id         UUID NOT NULL,
    service_date       DATE,
    description        TEXT NOT NULL,
    quantity           NUMERIC(14,4) NOT NULL DEFAULT 1,
    unit               TEXT,
    unit_price         NUMERIC(14,4) NOT NULL DEFAULT 0,
    tax_rate           NUMERIC(5,2)  NOT NULL DEFAULT 0.00,
    tax_amount         NUMERIC(14,4) NOT NULL DEFAULT 0,
    service_code       TEXT,
    external_ref_type  TEXT,
    external_ref_id    TEXT,
    CONSTRAINT fk_invoice_lines_invoice
    FOREIGN KEY (invoice_id) REFERENCES billing.invoices(id) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS billing.external_links (
                                                      id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID NOT NULL,
    invoice_id       UUID,
    invoice_line_id  UUID,
    ref_type         TEXT NOT NULL,
    ref_id           TEXT NOT NULL,
    note             TEXT,
    CONSTRAINT ck_xor_invoice_vs_line CHECK (
(invoice_id IS NOT NULL) <> (invoice_line_id IS NOT NULL)
    ),
    CONSTRAINT fk_extlink_invoice
    FOREIGN KEY (invoice_id)      REFERENCES billing.invoices(id)      ON DELETE CASCADE,
    CONSTRAINT fk_extlink_invoice_line
    FOREIGN KEY (invoice_line_id) REFERENCES billing.invoice_lines(id) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS billing.meter_readings (
                                                      id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL,
    unit_id       UUID NOT NULL,
    service_code  TEXT NOT NULL,
    reading_date  DATE NOT NULL,
    prev_index    NUMERIC(14,3) NOT NULL,
    curr_index    NUMERIC(14,3) NOT NULL,
    photo_file_id UUID,
    note          TEXT,
    CONSTRAINT ck_reading_nonneg CHECK (curr_index >= 0 AND prev_index >= 0 AND curr_index >= prev_index),
    CONSTRAINT uq_meter_reading UNIQUE (tenant_id, unit_id, service_code, reading_date)
    );

CREATE TABLE IF NOT EXISTS billing.meter_charge_links (
                                                          id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID NOT NULL,
    invoice_line_id  UUID NOT NULL,
    reading_id       UUID NOT NULL,
    CONSTRAINT fk_mcl_invoice_line
    FOREIGN KEY (invoice_line_id) REFERENCES billing.invoice_lines(id) ON DELETE CASCADE,
    CONSTRAINT fk_mcl_reading
    FOREIGN KEY (reading_id)      REFERENCES billing.meter_readings(id) ON DELETE CASCADE,
    CONSTRAINT uq_mcl_invoice_line UNIQUE (tenant_id, invoice_line_id),
    CONSTRAINT uq_mcl_reading      UNIQUE (tenant_id, reading_id)
    );

CREATE INDEX IF NOT EXISTS idx_invoices_tenant_status ON billing.invoices (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_invoice_lines_invoice ON billing.invoice_lines (invoice_id);
CREATE INDEX IF NOT EXISTS idx_meter_readings_unit_service_date ON billing.meter_readings (tenant_id, unit_id, service_code, reading_date DESC);
