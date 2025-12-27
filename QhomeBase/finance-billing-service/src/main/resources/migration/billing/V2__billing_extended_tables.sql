DO $do$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_type t
    JOIN pg_namespace n ON n.oid = t.typnamespace
    WHERE t.typname = 'adjustment_type' AND n.nspname = 'billing'
  ) THEN
CREATE TYPE billing.adjustment_type AS ENUM ('DISCOUNT', 'PROMOTION', 'CORRECTION', 'REFUND');
END IF;
END
$do$;

DO $do$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_type t
    JOIN pg_namespace n ON n.oid = t.typnamespace
    WHERE t.typname = 'penalty_type' AND n.nspname = 'billing'
  ) THEN
CREATE TYPE billing.penalty_type AS ENUM ('PERCENTAGE', 'FIXED_AMOUNT');
END IF;
END
$do$;

DO $do$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_type t
    JOIN pg_namespace n ON n.oid = t.typnamespace
    WHERE t.typname = 'reminder_type' AND n.nspname = 'billing'
  ) THEN
CREATE TYPE billing.reminder_type AS ENUM ('EMAIL', 'SMS', 'NOTIFICATION', 'IN_APP');
END IF;
END
$do$;

DO $do$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_type t
    JOIN pg_namespace n ON n.oid = t.typnamespace
    WHERE t.typname = 'reminder_status' AND n.nspname = 'billing'
  ) THEN
CREATE TYPE billing.reminder_status AS ENUM ('PENDING', 'SENT', 'FAILED', 'CANCELLED');
END IF;
END
$do$;

CREATE TABLE IF NOT EXISTS billing.service_pricing (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL,
    service_code      TEXT NOT NULL,
    service_name      TEXT NOT NULL,
    category          TEXT,
    base_price        NUMERIC(14,4) NOT NULL,
    unit              TEXT NOT NULL,
    tax_rate          NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    effective_from    DATE NOT NULL,
    effective_until   DATE,
    active            BOOLEAN NOT NULL DEFAULT true,
    description       TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        UUID,
    updated_by        UUID,
    
    CONSTRAINT uq_service_pricing_code_date 
        UNIQUE (tenant_id, service_code, effective_from),
    CONSTRAINT ck_price_positive 
        CHECK (base_price >= 0),
    CONSTRAINT ck_tax_rate_valid
        CHECK (tax_rate >= 0 AND tax_rate <= 100),
    CONSTRAINT ck_effective_date_range
        CHECK (effective_until IS NULL OR effective_until >= effective_from)
);

CREATE TABLE IF NOT EXISTS billing.late_payment_config (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL,
    days_overdue      INTEGER NOT NULL,
    penalty_type      billing.penalty_type NOT NULL,
    penalty_value     NUMERIC(14,4) NOT NULL,
    active            BOOLEAN NOT NULL DEFAULT true,
    description       TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        UUID,
    
    CONSTRAINT uq_late_config_days 
        UNIQUE (tenant_id, days_overdue),
    CONSTRAINT ck_days_positive 
        CHECK (days_overdue > 0),
    CONSTRAINT ck_penalty_positive 
        CHECK (penalty_value > 0),
    CONSTRAINT ck_percentage_valid
        CHECK (penalty_type != 'PERCENTAGE' OR penalty_value <= 100)
);

CREATE TABLE IF NOT EXISTS billing.late_payment_charges (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL,
    invoice_id        UUID NOT NULL,
    config_id         UUID NOT NULL,
    days_overdue      INTEGER NOT NULL,
    penalty_amount    NUMERIC(14,4) NOT NULL,
    charged_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    waived            BOOLEAN NOT NULL DEFAULT false,
    waived_by         UUID,
    waived_at         TIMESTAMPTZ,
    waive_reason      TEXT,
    
    CONSTRAINT fk_late_invoice 
        FOREIGN KEY (invoice_id) REFERENCES billing.invoices(id) ON DELETE CASCADE,
    CONSTRAINT fk_late_config 
        FOREIGN KEY (config_id) REFERENCES billing.late_payment_config(id) ON DELETE RESTRICT,
    CONSTRAINT ck_penalty_amount_positive
        CHECK (penalty_amount >= 0)
);

CREATE TABLE IF NOT EXISTS billing.invoice_adjustments (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL,
    invoice_id        UUID NOT NULL,
    adjustment_type   billing.adjustment_type NOT NULL,
    amount            NUMERIC(14,4) NOT NULL,
    reason            TEXT NOT NULL,
    approved_by       UUID,
    approved_at       TIMESTAMPTZ,
    created_by        UUID NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT fk_adj_invoice 
        FOREIGN KEY (invoice_id) REFERENCES billing.invoices(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS billing.payment_reminders (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL,
    invoice_id        UUID NOT NULL,
    reminder_type     billing.reminder_type NOT NULL,
    days_before_due   INTEGER,
    days_after_due    INTEGER,
    scheduled_at      TIMESTAMPTZ NOT NULL,
    sent_at           TIMESTAMPTZ,
    status            billing.reminder_status NOT NULL DEFAULT 'PENDING',
    recipient_email   TEXT,
    recipient_phone   TEXT,
    message_template  TEXT,
    error_message     TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT fk_reminder_invoice 
        FOREIGN KEY (invoice_id) REFERENCES billing.invoices(id) ON DELETE CASCADE,
    CONSTRAINT ck_reminder_days
        CHECK ((days_before_due IS NOT NULL AND days_before_due > 0) OR 
               (days_after_due IS NOT NULL AND days_after_due >= 0))
);

CREATE INDEX IF NOT EXISTS idx_service_pricing_tenant_code ON billing.service_pricing (tenant_id, service_code);
CREATE INDEX IF NOT EXISTS idx_service_pricing_active ON billing.service_pricing (tenant_id, active, effective_from);
CREATE INDEX IF NOT EXISTS idx_late_charges_invoice ON billing.late_payment_charges (invoice_id);
CREATE INDEX IF NOT EXISTS idx_late_charges_tenant_date ON billing.late_payment_charges (tenant_id, charged_at DESC);
CREATE INDEX IF NOT EXISTS idx_adjustments_invoice ON billing.invoice_adjustments (invoice_id);
CREATE INDEX IF NOT EXISTS idx_adjustments_tenant_date ON billing.invoice_adjustments (tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_reminders_invoice ON billing.payment_reminders (invoice_id);
CREATE INDEX IF NOT EXISTS idx_reminders_status_scheduled ON billing.payment_reminders (status, scheduled_at);
CREATE INDEX IF NOT EXISTS idx_reminders_tenant_status ON billing.payment_reminders (tenant_id, status);

COMMENT ON TABLE billing.service_pricing IS 'Bảng giá dịch vụ (phí gửi xe, điện, nước, dịch vụ chung...)';
COMMENT ON TABLE billing.late_payment_config IS 'Cấu hình phí trễ hạn theo số ngày';
COMMENT ON TABLE billing.late_payment_charges IS 'Lịch sử tính phí trễ hạn';
COMMENT ON TABLE billing.invoice_adjustments IS 'Điều chỉnh hóa đơn (giảm giá, sửa lỗi, hoàn tiền)';
COMMENT ON TABLE billing.payment_reminders IS 'Nhắc nhở thanh toán tự động';

COMMENT ON COLUMN billing.service_pricing.service_code IS 'Mã dịch vụ: PARKING_CAR, PARKING_MOTORBIKE, WATER, ELECTRIC, MAINTENANCE...';
COMMENT ON COLUMN billing.service_pricing.category IS 'Nhóm: PARKING, UTILITIES, MAINTENANCE, SERVICE...';
COMMENT ON COLUMN billing.service_pricing.unit IS 'Đơn vị tính: month, kWh, m3, unit...';
COMMENT ON COLUMN billing.late_payment_config.penalty_type IS 'PERCENTAGE: % trên tổng tiền, FIXED_AMOUNT: số tiền cố định';
COMMENT ON COLUMN billing.late_payment_charges.waived IS 'true nếu đã miễn giảm phí phạt';
COMMENT ON COLUMN billing.payment_reminders.days_before_due IS 'Nhắc trước X ngày đến hạn (NULL nếu nhắc sau khi quá hạn)';
COMMENT ON COLUMN billing.payment_reminders.days_after_due IS 'Nhắc sau X ngày quá hạn (NULL nếu nhắc trước khi đến hạn)';
