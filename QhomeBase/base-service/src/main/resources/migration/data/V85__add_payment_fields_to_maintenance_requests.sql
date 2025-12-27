-- V85: Add payment fields to maintenance_requests table
-- This allows maintenance requests to be paid via VNPay

DO $$
BEGIN
    -- Add payment_status column if not exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'data' 
        AND table_name = 'maintenance_requests' 
        AND column_name = 'payment_status'
    ) THEN
        ALTER TABLE data.maintenance_requests
            ADD COLUMN payment_status VARCHAR(50) DEFAULT 'UNPAID';
        RAISE NOTICE 'Added payment_status column';
    END IF;

    -- Add payment_amount column if not exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'data' 
        AND table_name = 'maintenance_requests' 
        AND column_name = 'payment_amount'
    ) THEN
        ALTER TABLE data.maintenance_requests
            ADD COLUMN payment_amount NUMERIC(15, 2);
        RAISE NOTICE 'Added payment_amount column';
    END IF;

    -- Add payment_date column if not exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'data' 
        AND table_name = 'maintenance_requests' 
        AND column_name = 'payment_date'
    ) THEN
        ALTER TABLE data.maintenance_requests
            ADD COLUMN payment_date TIMESTAMPTZ;
        RAISE NOTICE 'Added payment_date column';
    END IF;

    -- Add payment_gateway column if not exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'data' 
        AND table_name = 'maintenance_requests' 
        AND column_name = 'payment_gateway'
    ) THEN
        ALTER TABLE data.maintenance_requests
            ADD COLUMN payment_gateway VARCHAR(50);
        RAISE NOTICE 'Added payment_gateway column';
    END IF;

    -- Add vnpay_transaction_ref column if not exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'data' 
        AND table_name = 'maintenance_requests' 
        AND column_name = 'vnpay_transaction_ref'
    ) THEN
        ALTER TABLE data.maintenance_requests
            ADD COLUMN vnpay_transaction_ref VARCHAR(255);
        RAISE NOTICE 'Added vnpay_transaction_ref column';
    END IF;
END $$;

-- Add indexes for payment fields
CREATE INDEX IF NOT EXISTS idx_maintenance_requests_payment_status 
    ON data.maintenance_requests(payment_status);
CREATE INDEX IF NOT EXISTS idx_maintenance_requests_vnpay_transaction_ref 
    ON data.maintenance_requests(vnpay_transaction_ref);

-- Add comments
COMMENT ON COLUMN data.maintenance_requests.payment_status IS 'Payment status: UNPAID, PAID, FAILED';
COMMENT ON COLUMN data.maintenance_requests.payment_amount IS 'Amount paid for maintenance request';
COMMENT ON COLUMN data.maintenance_requests.payment_date IS 'Date when payment was completed';
COMMENT ON COLUMN data.maintenance_requests.payment_gateway IS 'Payment gateway used (VNPAY, etc.)';
COMMENT ON COLUMN data.maintenance_requests.vnpay_transaction_ref IS 'VNPay transaction reference for tracking payment';

