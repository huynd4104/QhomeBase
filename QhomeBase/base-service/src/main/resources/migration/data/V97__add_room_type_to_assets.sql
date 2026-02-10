-- Migration: Add room_type to assets and update asset_type enum
-- This migration adds support for grouping assets by room type

-- Create room_type enum
CREATE TYPE data.room_type AS ENUM (
    'BATHROOM',
    'LIVING_ROOM',
    'BEDROOM',
    'KITCHEN',
    'HALLWAY'
);

-- Add new values to asset_type enum
-- Thiết bị nhà tắm
ALTER TYPE data.asset_type ADD VALUE IF NOT EXISTS 'TOILET';
ALTER TYPE data.asset_type ADD VALUE IF NOT EXISTS 'BATHROOM_SINK';
ALTER TYPE data.asset_type ADD VALUE IF NOT EXISTS 'SHOWER_SYSTEM';
ALTER TYPE data.asset_type ADD VALUE IF NOT EXISTS 'BATHROOM_FAUCET';
ALTER TYPE data.asset_type ADD VALUE IF NOT EXISTS 'BATHROOM_LIGHT';
ALTER TYPE data.asset_type ADD VALUE IF NOT EXISTS 'BATHROOM_DOOR';
ALTER TYPE data.asset_type ADD VALUE IF NOT EXISTS 'BATHROOM_ELECTRICAL';

-- Thiết bị phòng khách
ALTER TYPE data.asset_type ADD VALUE IF NOT EXISTS 'LIVING_ROOM_DOOR';
ALTER TYPE data.asset_type ADD VALUE IF NOT EXISTS 'LIVING_ROOM_LIGHT';
ALTER TYPE data.asset_type ADD VALUE IF NOT EXISTS 'INTERNET_SYSTEM';
ALTER TYPE data.asset_type ADD VALUE IF NOT EXISTS 'FAN';
ALTER TYPE data.asset_type ADD VALUE IF NOT EXISTS 'LIVING_ROOM_ELECTRICAL';

-- Thiết bị phòng ngủ
ALTER TYPE data.asset_type ADD VALUE IF NOT EXISTS 'BEDROOM_ELECTRICAL';
ALTER TYPE data.asset_type ADD VALUE IF NOT EXISTS 'BEDROOM_AIR_CONDITIONER';
ALTER TYPE data.asset_type ADD VALUE IF NOT EXISTS 'BEDROOM_DOOR';
ALTER TYPE data.asset_type ADD VALUE IF NOT EXISTS 'BEDROOM_WINDOW';

-- Thiết bị nhà bếp
ALTER TYPE data.asset_type ADD VALUE IF NOT EXISTS 'KITCHEN_LIGHT';
ALTER TYPE data.asset_type ADD VALUE IF NOT EXISTS 'KITCHEN_ELECTRICAL';
ALTER TYPE data.asset_type ADD VALUE IF NOT EXISTS 'ELECTRIC_STOVE';
ALTER TYPE data.asset_type ADD VALUE IF NOT EXISTS 'KITCHEN_DOOR';

-- Thiết bị hành lang
ALTER TYPE data.asset_type ADD VALUE IF NOT EXISTS 'HALLWAY_LIGHT';
ALTER TYPE data.asset_type ADD VALUE IF NOT EXISTS 'HALLWAY_ELECTRICAL';

-- Add room_type column to assets table (nullable initially for existing data)
ALTER TABLE data.assets ADD COLUMN IF NOT EXISTS room_type data.room_type;

-- Update existing assets with default room types based on current asset_type
UPDATE data.assets SET room_type = 'BATHROOM' WHERE asset_type = 'WATER_HEATER' AND room_type IS NULL;
UPDATE data.assets SET room_type = 'LIVING_ROOM' WHERE asset_type = 'AIR_CONDITIONER' AND room_type IS NULL;
UPDATE data.assets SET room_type = 'KITCHEN' WHERE asset_type = 'KITCHEN' AND room_type IS NULL;
UPDATE data.assets SET room_type = 'LIVING_ROOM' WHERE room_type IS NULL; -- Default for remaining

-- Make room_type NOT NULL after setting defaults
ALTER TABLE data.assets ALTER COLUMN room_type SET NOT NULL;

-- Create index for room_type queries
CREATE INDEX IF NOT EXISTS idx_assets_room_type ON data.assets(room_type);
CREATE INDEX IF NOT EXISTS idx_assets_unit_room_type ON data.assets(unit_id, room_type);
