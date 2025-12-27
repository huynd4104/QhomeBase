-- Fix records where payment_status = PAYMENT_FAILED but status is still PAYMENT_PENDING
-- These records were expired by PaymentPendingExpiryJob but status was not updated properly
-- This migration fixes the status to PAYMENT_FAILED to match payment_status

-- Vehicle registrations
UPDATE card.register_vehicle
SET status = 'PAYMENT_FAILED'
WHERE payment_status = 'PAYMENT_FAILED'
  AND status = 'PAYMENT_PENDING'
  AND (admin_note LIKE '%Thanh toán VNPay quá thời gian%' 
       OR admin_note LIKE '%quá thời gian%');

-- Resident card registrations
UPDATE card.resident_card_registration
SET status = 'PAYMENT_FAILED'
WHERE payment_status = 'PAYMENT_FAILED'
  AND status = 'PAYMENT_PENDING'
  AND (admin_note LIKE '%Thanh toán VNPay quá thời gian%' 
       OR admin_note LIKE '%quá thời gian%');

-- Elevator card registrations
UPDATE card.elevator_card_registration
SET status = 'PAYMENT_FAILED'
WHERE payment_status = 'PAYMENT_FAILED'
  AND status = 'PAYMENT_PENDING'
  AND (admin_note LIKE '%Thanh toán VNPay quá thời gian%' 
       OR admin_note LIKE '%quá thời gian%');
