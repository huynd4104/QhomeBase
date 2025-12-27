ALTER TABLE card.resident_card_registration
    ADD COLUMN IF NOT EXISTS full_name TEXT,
    ADD COLUMN IF NOT EXISTS apartment_number TEXT,
    ADD COLUMN IF NOT EXISTS building_name TEXT,
    ADD COLUMN IF NOT EXISTS citizen_id TEXT;


