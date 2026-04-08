-- V99: Refactor asset_type enum (device-only, no room prefix) + add soft delete support
-- AssetType will now be purely device types. Room info comes from the existing room_type column.

-- ============================================================
-- PART 1: Soft delete columns
-- ============================================================
ALTER TABLE data.assets ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE data.assets ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_assets_deleted ON data.assets(deleted);

-- ============================================================
-- PART 2: Add 'OTHER' to room_type enum if not exists
-- (already exists based on Java enum, but ensure DB has it)
-- ============================================================
ALTER TYPE data.room_type ADD VALUE IF NOT EXISTS 'OTHER';

-- ============================================================
-- PART 3: Refactor asset_type enum
-- Step 1: Convert asset_type column to TEXT
-- ============================================================
ALTER TABLE data.assets ALTER COLUMN asset_type TYPE TEXT USING asset_type::TEXT;

-- Also convert asset_inspections if it uses asset_type
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'data' AND table_name = 'asset_inspections' AND column_name = 'asset_type') THEN
ALTER TABLE data.asset_inspections ALTER COLUMN asset_type TYPE TEXT USING asset_type::TEXT;
END IF;
END $$;

-- ============================================================
-- Step 2: Map old room-specific values to new device-only values
-- ============================================================
-- Lights: all *_LIGHT -> LIGHT
UPDATE data.assets SET asset_type = 'LIGHT'
WHERE asset_type IN ('BATHROOM_LIGHT', 'LIVING_ROOM_LIGHT', 'KITCHEN_LIGHT', 'HALLWAY_LIGHT');

-- Doors: all *_DOOR -> DOOR
UPDATE data.assets SET asset_type = 'DOOR'
WHERE asset_type IN ('BATHROOM_DOOR', 'LIVING_ROOM_DOOR', 'BEDROOM_DOOR', 'KITCHEN_DOOR');

-- Electrical systems: all *_ELECTRICAL -> ELECTRICAL_SYSTEM
UPDATE data.assets SET asset_type = 'ELECTRICAL_SYSTEM'
WHERE asset_type IN ('BATHROOM_ELECTRICAL', 'LIVING_ROOM_ELECTRICAL', 'BEDROOM_ELECTRICAL',
                     'KITCHEN_ELECTRICAL', 'HALLWAY_ELECTRICAL');

-- Air conditioners: BEDROOM_AIR_CONDITIONER -> AIR_CONDITIONER
UPDATE data.assets SET asset_type = 'AIR_CONDITIONER'
WHERE asset_type = 'BEDROOM_AIR_CONDITIONER';

-- Windows: BEDROOM_WINDOW -> WINDOW
UPDATE data.assets SET asset_type = 'WINDOW'
WHERE asset_type = 'BEDROOM_WINDOW';

-- Sinks: BATHROOM_SINK -> SINK
UPDATE data.assets SET asset_type = 'SINK'
WHERE asset_type = 'BATHROOM_SINK';

-- Faucets: BATHROOM_FAUCET -> FAUCET
UPDATE data.assets SET asset_type = 'FAUCET'
WHERE asset_type = 'BATHROOM_FAUCET';

-- Map old generic types to OTHER
UPDATE data.assets SET asset_type = 'OTHER'
WHERE asset_type IN ('KITCHEN', 'FURNITURE')
  AND asset_type NOT IN ('TOILET', 'SINK', 'WATER_HEATER', 'SHOWER_SYSTEM', 'FAUCET',
                         'LIGHT', 'DOOR', 'WINDOW', 'ELECTRICAL_SYSTEM',
                         'AIR_CONDITIONER', 'INTERNET_SYSTEM', 'FAN', 'ELECTRIC_STOVE', 'OTHER');

-- Catch-all: anything not in the new enum -> OTHER
UPDATE data.assets SET asset_type = 'OTHER'
WHERE asset_type NOT IN ('TOILET', 'SINK', 'WATER_HEATER', 'SHOWER_SYSTEM', 'FAUCET',
                         'LIGHT', 'DOOR', 'WINDOW', 'ELECTRICAL_SYSTEM',
                         'AIR_CONDITIONER', 'INTERNET_SYSTEM', 'FAN', 'ELECTRIC_STOVE', 'OTHER');

-- ============================================================
-- Step 3: Drop old enum and create new one
-- ============================================================
DROP TYPE IF EXISTS data.asset_type;

CREATE TYPE data.asset_type AS ENUM (
    'TOILET',
    'SINK',
    'WATER_HEATER',
    'SHOWER_SYSTEM',
    'FAUCET',
    'LIGHT',
    'DOOR',
    'WINDOW',
    'ELECTRICAL_SYSTEM',
    'AIR_CONDITIONER',
    'INTERNET_SYSTEM',
    'FAN',
    'ELECTRIC_STOVE',
    'OTHER'
);

-- ============================================================
-- Step 4: Convert columns back to enum
-- ============================================================
ALTER TABLE data.assets
ALTER COLUMN asset_type TYPE data.asset_type USING asset_type::data.asset_type;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'data' AND table_name = 'asset_inspections' AND column_name = 'asset_type') THEN
        -- Also update asset_inspections data if needed
UPDATE data.asset_inspections SET asset_type = 'LIGHT'
WHERE asset_type IN ('BATHROOM_LIGHT', 'LIVING_ROOM_LIGHT', 'KITCHEN_LIGHT', 'HALLWAY_LIGHT');
UPDATE data.asset_inspections SET asset_type = 'DOOR'
WHERE asset_type IN ('BATHROOM_DOOR', 'LIVING_ROOM_DOOR', 'BEDROOM_DOOR', 'KITCHEN_DOOR');
UPDATE data.asset_inspections SET asset_type = 'ELECTRICAL_SYSTEM'
WHERE asset_type IN ('BATHROOM_ELECTRICAL', 'LIVING_ROOM_ELECTRICAL', 'BEDROOM_ELECTRICAL',
                     'KITCHEN_ELECTRICAL', 'HALLWAY_ELECTRICAL');
UPDATE data.asset_inspections SET asset_type = 'AIR_CONDITIONER'
WHERE asset_type = 'BEDROOM_AIR_CONDITIONER';
UPDATE data.asset_inspections SET asset_type = 'WINDOW'
WHERE asset_type = 'BEDROOM_WINDOW';
UPDATE data.asset_inspections SET asset_type = 'SINK'
WHERE asset_type = 'BATHROOM_SINK';
UPDATE data.asset_inspections SET asset_type = 'FAUCET'
WHERE asset_type = 'BATHROOM_FAUCET';
UPDATE data.asset_inspections SET asset_type = 'OTHER'
WHERE asset_type NOT IN ('TOILET', 'SINK', 'WATER_HEATER', 'SHOWER_SYSTEM', 'FAUCET',
                         'LIGHT', 'DOOR', 'WINDOW', 'ELECTRICAL_SYSTEM',
                         'AIR_CONDITIONER', 'INTERNET_SYSTEM', 'FAN', 'ELECTRIC_STOVE', 'OTHER');

ALTER TABLE data.asset_inspections
ALTER COLUMN asset_type TYPE data.asset_type USING asset_type::data.asset_type;
END IF;
END $$;

COMMENT ON TYPE data.asset_type IS 'Loại thiết bị: Bồn cầu, Chậu rửa, Bình nóng lạnh, Sen vòi, Vòi nước, Đèn, Cửa, Cửa sổ, Hệ thống điện, Điều hoà, Internet, Quạt, Bếp điện, Khác';
