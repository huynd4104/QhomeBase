ALTER TABLE card.elevator_card_registration
    ADD COLUMN IF NOT EXISTS full_name TEXT,
    ADD COLUMN IF NOT EXISTS apartment_number TEXT,
    ADD COLUMN IF NOT EXISTS building_name TEXT,
    ADD COLUMN IF NOT EXISTS citizen_id TEXT;

ALTER TABLE card.elevator_card_registration
    ALTER COLUMN unit_id DROP NOT NULL,
    ALTER COLUMN resident_id DROP NOT NULL,
    ALTER COLUMN payment_amount SET DEFAULT 30000;

UPDATE card.elevator_card_registration
SET payment_amount = 30000
WHERE payment_amount IS NULL;

