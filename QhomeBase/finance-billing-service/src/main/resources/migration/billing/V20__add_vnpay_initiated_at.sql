-- Add vnpay_initiated_at column to track when VNPay payment was initiated
-- This is used to auto-expire pending VNPay payments after 10 minutes

ALTER TABLE billing.invoices
ADD COLUMN IF NOT EXISTS vnpay_initiated_at TIMESTAMPTZ;

COMMENT ON COLUMN billing.invoices.vnpay_initiated_at IS 'Thời điểm bắt đầu thanh toán VNPay. Dùng để tự động expire payment quá 10 phút';

-- Create index for efficient querying of pending VNPay payments
CREATE INDEX IF NOT EXISTS idx_invoices_vnpay_initiated 
ON billing.invoices(vnpay_initiated_at) 
WHERE vnpay_initiated_at IS NOT NULL AND status != 'PAID' AND payment_gateway = 'VNPAY';
