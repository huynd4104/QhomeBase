-- V21: Create paid_service_history table to track internal services payment history
-- This table stores history of paid internal services (electric, water, parking, etc.)
-- Used to display paid services on homescreen

CREATE TABLE IF NOT EXISTS billing.paid_service_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL,
    invoice_code VARCHAR(100),
    service_code VARCHAR(50) NOT NULL,
    service_name VARCHAR(255) NOT NULL,
    unit_id UUID NOT NULL,
    resident_id UUID,
    amount NUMERIC(14, 4) NOT NULL,
    payment_gateway VARCHAR(50),
    paid_at TIMESTAMP WITH TIME ZONE NOT NULL,
    payment_month INTEGER NOT NULL,
    payment_year INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_paid_service_invoice FOREIGN KEY (invoice_id) REFERENCES billing.invoices(id) ON DELETE CASCADE
);

-- Index for querying by unit and payment period (for homescreen display)
CREATE INDEX IF NOT EXISTS idx_paid_service_unit_period 
    ON billing.paid_service_history(unit_id, payment_year, payment_month, paid_at DESC);

-- Index for querying by resident
CREATE INDEX IF NOT EXISTS idx_paid_service_resident 
    ON billing.paid_service_history(resident_id, paid_at DESC);

-- Index for querying by service code
CREATE INDEX IF NOT EXISTS idx_paid_service_code 
    ON billing.paid_service_history(service_code, unit_id, payment_year, payment_month);

COMMENT ON TABLE billing.paid_service_history IS 
'Lịch sử thanh toán các dịch vụ nội bộ (điện, nước, gửi xe...). Dùng để hiển thị trên homescreen những dịch vụ user đã thanh toán trong tháng';

COMMENT ON COLUMN billing.paid_service_history.payment_month IS 'Tháng thanh toán (1-12)';
COMMENT ON COLUMN billing.paid_service_history.payment_year IS 'Năm thanh toán';

