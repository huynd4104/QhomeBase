ALTER TABLE card.resident_card_registration
    ADD COLUMN IF NOT EXISTS admin_note TEXT,
    ADD COLUMN IF NOT EXISTS approved_by UUID,
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS rejection_reason TEXT;

UPDATE card.resident_card_registration
SET status = 'PENDING_APPROVAL'
WHERE status IS NULL
   OR status = ''
   OR status = 'PENDING';

