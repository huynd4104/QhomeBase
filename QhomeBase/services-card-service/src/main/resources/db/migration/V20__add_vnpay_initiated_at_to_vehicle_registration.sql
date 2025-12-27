-- Add vnpay_initiated_at column to track when VNPay payment was initiated for vehicle cards
-- This is used to expire pending payments after 10 minutes

ALTER TABLE card.register_vehicle
ADD COLUMN IF NOT EXISTS vnpay_initiated_at TIMESTAMPTZ;

-- Create index for efficient querying of expired payments
CREATE INDEX IF NOT EXISTS idx_vehicle_card_vnpay_initiated
ON card.register_vehicle(vnpay_initiated_at)
WHERE vnpay_initiated_at IS NOT NULL AND payment_status = 'PAYMENT_IN_PROGRESS';
