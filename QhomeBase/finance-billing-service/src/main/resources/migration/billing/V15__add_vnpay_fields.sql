-- Add VNPAY tracking fields to invoices table
ALTER TABLE IF EXISTS billing.invoices
    ADD COLUMN IF NOT EXISTS payment_gateway VARCHAR(50),
    ADD COLUMN IF NOT EXISTS vnp_transaction_ref VARCHAR(100),
    ADD COLUMN IF NOT EXISTS vnp_transaction_no VARCHAR(100),
    ADD COLUMN IF NOT EXISTS vnp_bank_code VARCHAR(50),
    ADD COLUMN IF NOT EXISTS vnp_card_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS vnp_response_code VARCHAR(10),
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMPTZ;


