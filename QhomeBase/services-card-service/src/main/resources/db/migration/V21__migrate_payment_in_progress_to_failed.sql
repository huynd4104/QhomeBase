-- Migrate all PAYMENT_IN_PROGRESS status to PAYMENT_FAILED
-- This updates existing cards that are stuck in "Thanh toán đang xử lý" status
-- to "Thanh toán không thành công" so users can retry payment

-- Update resident card registrations
UPDATE card.resident_card_registration
SET payment_status = 'PAYMENT_FAILED',
    admin_note = COALESCE(
        admin_note || E'\n' || 'Đã chuyển từ trạng thái thanh toán đang xử lý sang thanh toán không thành công',
        'Đã chuyển từ trạng thái thanh toán đang xử lý sang thanh toán không thành công'
    )
WHERE UPPER(payment_status) = 'PAYMENT_IN_PROGRESS'
  AND payment_status != 'PAID';

-- Update elevator card registrations
UPDATE card.elevator_card_registration
SET payment_status = 'PAYMENT_FAILED',
    admin_note = COALESCE(
        admin_note || E'\n' || 'Đã chuyển từ trạng thái thanh toán đang xử lý sang thanh toán không thành công',
        'Đã chuyển từ trạng thái thanh toán đang xử lý sang thanh toán không thành công'
    )
WHERE UPPER(payment_status) = 'PAYMENT_IN_PROGRESS'
  AND payment_status != 'PAID';

-- Update vehicle card registrations
UPDATE card.register_vehicle
SET payment_status = 'PAYMENT_FAILED',
    admin_note = COALESCE(
        admin_note || E'\n' || 'Đã chuyển từ trạng thái thanh toán đang xử lý sang thanh toán không thành công',
        'Đã chuyển từ trạng thái thanh toán đang xử lý sang thanh toán không thành công'
    )
WHERE UPPER(payment_status) = 'PAYMENT_IN_PROGRESS'
  AND payment_status != 'PAID';

-- Also migrate legacy PAYMENT_APPROVAL status for vehicle cards
UPDATE card.register_vehicle
SET payment_status = 'PAYMENT_FAILED',
    admin_note = COALESCE(
        admin_note || E'\n' || 'Đã chuyển từ trạng thái PAYMENT_APPROVAL sang thanh toán không thành công',
        'Đã chuyển từ trạng thái PAYMENT_APPROVAL sang thanh toán không thành công'
    )
WHERE UPPER(payment_status) = 'PAYMENT_APPROVAL'
  AND payment_status != 'PAID';
