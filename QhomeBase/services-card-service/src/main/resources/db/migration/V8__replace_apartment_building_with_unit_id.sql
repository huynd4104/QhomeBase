-- V8: Replace apartment_number, building_name, and citizen_id with unit_id and resident_id
-- This migration changes from storing text values to using foreign key references

-- Step 1: Add unit_id and resident_id columns to both tables
ALTER TABLE card.resident_card_registration
    ADD COLUMN IF NOT EXISTS unit_id UUID;

ALTER TABLE card.resident_card_registration
    ADD COLUMN IF NOT EXISTS resident_id UUID;

ALTER TABLE card.elevator_card_registration
    ADD COLUMN IF NOT EXISTS unit_id UUID;

ALTER TABLE card.elevator_card_registration
    ADD COLUMN IF NOT EXISTS resident_id UUID;

-- Step 2: Drop old columns (apartment_number, building_name, and citizen_id)
ALTER TABLE card.resident_card_registration
    DROP COLUMN IF EXISTS apartment_number;

ALTER TABLE card.resident_card_registration
    DROP COLUMN IF EXISTS building_name;

ALTER TABLE card.resident_card_registration
    DROP COLUMN IF EXISTS citizen_id;

ALTER TABLE card.elevator_card_registration
    DROP COLUMN IF EXISTS apartment_number;

ALTER TABLE card.elevator_card_registration
    DROP COLUMN IF EXISTS building_name;

ALTER TABLE card.elevator_card_registration
    DROP COLUMN IF EXISTS citizen_id;

-- Note: phone_number is kept as nullable field for:
-- 1. Snapshot data (phone at registration time may differ from current resident phone)
-- 2. Alternative contact number (family member, representative)
-- 3. Convenience (avoid join with residents table)
-- If not provided, can be retrieved from residents table via resident_id

-- Step 3: Make unit_id NOT NULL (after data migration if needed)
-- Note: If you have existing data, you'll need to migrate it first
-- For now, we'll allow NULL temporarily, then you can update existing records
-- and add NOT NULL constraint in a separate migration

-- Step 4: Create indexes on unit_id and resident_id
CREATE INDEX IF NOT EXISTS idx_resident_card_registration_unit_id ON card.resident_card_registration(unit_id);
CREATE INDEX IF NOT EXISTS idx_resident_card_registration_resident_id ON card.resident_card_registration(resident_id);
CREATE INDEX IF NOT EXISTS idx_elevator_card_registration_unit_id ON card.elevator_card_registration(unit_id);
CREATE INDEX IF NOT EXISTS idx_elevator_card_registration_resident_id ON card.elevator_card_registration(resident_id);

-- Note: After migrating existing data, run:
-- ALTER TABLE card.resident_card_registration ALTER COLUMN unit_id SET NOT NULL;
-- ALTER TABLE card.resident_card_registration ALTER COLUMN resident_id SET NOT NULL;
-- ALTER TABLE card.elevator_card_registration ALTER COLUMN unit_id SET NOT NULL;
-- ALTER TABLE card.elevator_card_registration ALTER COLUMN resident_id SET NOT NULL;

