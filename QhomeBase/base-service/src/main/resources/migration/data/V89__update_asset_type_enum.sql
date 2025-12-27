-- V89: Update asset_type enum to only keep: AIR_CONDITIONER, KITCHEN, WATER_HEATER, FURNITURE, OTHER
-- Remove: REFRIGERATOR, WASHING_MACHINE, FAN, TELEVISION

-- Step 1: Change all columns using the enum to text temporarily first
-- This allows us to update values that may not exist in the enum
ALTER TABLE data.assets ALTER COLUMN asset_type TYPE TEXT USING asset_type::TEXT;

-- Check if asset_inspections table exists and uses asset_type (convert to text first)
DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'data' AND table_name = 'asset_inspections') THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'data' AND table_name = 'asset_inspections' AND column_name = 'asset_type') THEN
            ALTER TABLE data.asset_inspections ALTER COLUMN asset_type TYPE TEXT USING asset_type::TEXT;
        END IF;
    END IF;
END $$;

-- Step 2: Update existing assets with removed types to OTHER (now safe since it's TEXT)
UPDATE data.assets
SET asset_type = 'OTHER'
WHERE asset_type IN ('REFRIGERATOR', 'WASHING_MACHINE', 'FAN', 'TELEVISION');

-- Also update any invalid values to OTHER to ensure all values are valid for the new enum
UPDATE data.assets
SET asset_type = 'OTHER'
WHERE asset_type NOT IN ('AIR_CONDITIONER', 'KITCHEN', 'WATER_HEATER', 'FURNITURE', 'OTHER');

-- Step 3: Drop the old enum
DROP TYPE IF EXISTS data.asset_type;

-- Create new enum with only the 5 types
CREATE TYPE data.asset_type AS ENUM (
    'AIR_CONDITIONER',
    'KITCHEN',
    'WATER_HEATER',
    'FURNITURE',
    'OTHER'
);

-- Change the columns back to use the new enum
ALTER TABLE data.assets 
    ALTER COLUMN asset_type TYPE data.asset_type USING asset_type::data.asset_type;

-- Update asset_inspections if it exists
DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'data' AND table_name = 'asset_inspections') THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'data' AND table_name = 'asset_inspections' AND column_name = 'asset_type') THEN
            ALTER TABLE data.asset_inspections 
                ALTER COLUMN asset_type TYPE data.asset_type USING asset_type::data.asset_type;
        END IF;
    END IF;
END $$;

-- Add comment
COMMENT ON TYPE data.asset_type IS 'Loại tài sản: Điều hòa, Bếp, Nóng lạnh, Nội thất, Khác';

