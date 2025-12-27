ALTER TABLE data.asset_inspections
ADD COLUMN IF NOT EXISTS scheduled_date DATE;

CREATE INDEX IF NOT EXISTS idx_asset_inspections_scheduled_date ON data.asset_inspections (scheduled_date);

