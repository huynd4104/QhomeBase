ALTER TABLE card.register_vehicle
    ADD COLUMN IF NOT EXISTS apartment_number TEXT,
    ADD COLUMN IF NOT EXISTS building_name TEXT;


