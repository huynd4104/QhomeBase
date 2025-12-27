-- Migrate all PAYMENT_IN_PROGRESS status to PAYMENT_FAILED (V2)
-- This updates existing cards that are stuck in "Thanh toán đang xử lý" status
-- to "Thanh toán không thành công" so users can retry payment
-- 
-- Note: This is a duplicate migration to handle checksum mismatch from V21

-- Update resident card registrations
-- Chỉ update các thẻ có PAYMENT_IN_PROGRESS, không động vào PAYMENT_FAILED hoặc PAID
UPDATE card.resident_card_registration
SET payment_status = 'PAYMENT_FAILED',
    admin_note = COALESCE(
        admin_note || E'\n' || 'Đã chuyển từ trạng thái thanh toán đang xử lý sang thanh toán không thành công',
        'Đã chuyển từ trạng thái thanh toán đang xử lý sang thanh toán không thành công'
    )
WHERE UPPER(TRIM(payment_status)) = 'PAYMENT_IN_PROGRESS';

-- Update elevator card registrations
-- Chỉ update các thẻ có PAYMENT_IN_PROGRESS, không động vào PAYMENT_FAILED hoặc PAID
UPDATE card.elevator_card_registration
SET payment_status = 'PAYMENT_FAILED',
    admin_note = COALESCE(
        admin_note || E'\n' || 'Đã chuyển từ trạng thái thanh toán đang xử lý sang thanh toán không thành công',
        'Đã chuyển từ trạng thái thanh toán đang xử lý sang thanh toán không thành công'
    )
WHERE UPPER(TRIM(payment_status)) = 'PAYMENT_IN_PROGRESS';

-- Update vehicle card registrations
-- Chỉ update các thẻ có PAYMENT_IN_PROGRESS, không động vào PAYMENT_FAILED hoặc PAID
UPDATE card.register_vehicle
SET payment_status = 'PAYMENT_FAILED',
    admin_note = COALESCE(
        admin_note || E'\n' || 'Đã chuyển từ trạng thái thanh toán đang xử lý sang thanh toán không thành công',
        'Đã chuyển từ trạng thái thanh toán đang xử lý sang thanh toán không thành công'
    )
WHERE UPPER(TRIM(payment_status)) = 'PAYMENT_IN_PROGRESS';

-- Also migrate legacy PAYMENT_APPROVAL status for vehicle cards
-- Chỉ update các thẻ có PAYMENT_APPROVAL (legacy), không động vào PAYMENT_FAILED hoặc PAID
UPDATE card.register_vehicle
SET payment_status = 'PAYMENT_FAILED',
    admin_note = COALESCE(
        admin_note || E'\n' || 'Đã chuyển từ trạng thái PAYMENT_APPROVAL sang thanh toán không thành công',
        'Đã chuyển từ trạng thái PAYMENT_APPROVAL sang thanh toán không thành công'
    )
WHERE UPPER(TRIM(payment_status)) = 'PAYMENT_APPROVAL';
