-- V90: Add inspector_id column to asset_inspections table

-- Add inspector_id column
ALTER TABLE data.asset_inspections
ADD COLUMN IF NOT EXISTS inspector_id UUID;

-- Create index for inspector_id to improve query performance
CREATE INDEX IF NOT EXISTS idx_asset_inspections_inspector ON data.asset_inspections (inspector_id);

-- Add comment
COMMENT ON COLUMN data.asset_inspections.inspector_id IS 'ID của technician được gán để kiểm tra thiết bị';

