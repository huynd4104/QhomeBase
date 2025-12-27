-- Add vnpay_initiated_at column to track when VNPay payment was initiated
-- This is used to expire pending payments after 10 minutes

ALTER TABLE card.resident_card_registration 
ADD COLUMN IF NOT EXISTS vnpay_initiated_at TIMESTAMPTZ;

ALTER TABLE card.elevator_card_registration 
ADD COLUMN IF NOT EXISTS vnpay_initiated_at TIMESTAMPTZ;

-- Create index for efficient querying of expired payments
CREATE INDEX IF NOT EXISTS idx_resident_card_vnpay_initiated 
ON card.resident_card_registration(vnpay_initiated_at) 
WHERE vnpay_initiated_at IS NOT NULL AND payment_status = 'PAYMENT_IN_PROGRESS';

CREATE INDEX IF NOT EXISTS idx_elevator_card_vnpay_initiated 
ON card.elevator_card_registration(vnpay_initiated_at) 
WHERE vnpay_initiated_at IS NOT NULL AND payment_status = 'PAYMENT_IN_PROGRESS';
