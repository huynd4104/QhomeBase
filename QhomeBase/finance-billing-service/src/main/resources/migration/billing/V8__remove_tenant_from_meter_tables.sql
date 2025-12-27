-- V8: Remove tenant_id from meter-related tables
-- This migration removes tenant_id from meter_readings and meter_charge_links

-- Drop tenant-related constraints first
ALTER TABLE IF EXISTS billing.meter_readings DROP CONSTRAINT IF EXISTS uq_meter_reading;
ALTER TABLE IF EXISTS billing.meter_charge_links DROP CONSTRAINT IF EXISTS uq_mcl_invoice_line;
ALTER TABLE IF EXISTS billing.meter_charge_links DROP CONSTRAINT IF EXISTS uq_mcl_reading;

-- Drop tenant-related indexes
DROP INDEX IF EXISTS billing.idx_meter_readings_unit_service_date;

-- Drop tenant_id columns
ALTER TABLE IF EXISTS billing.meter_readings DROP COLUMN IF EXISTS tenant_id CASCADE;
ALTER TABLE IF EXISTS billing.meter_charge_links DROP COLUMN IF EXISTS tenant_id CASCADE;

-- Recreate unique constraints without tenant_id
ALTER TABLE billing.meter_readings 
    ADD CONSTRAINT uq_meter_reading UNIQUE (unit_id, service_code, reading_date);

ALTER TABLE billing.meter_charge_links 
    ADD CONSTRAINT uq_mcl_invoice_line UNIQUE (invoice_line_id);

ALTER TABLE billing.meter_charge_links 
    ADD CONSTRAINT uq_mcl_reading UNIQUE (reading_id);

-- Recreate index without tenant_id
CREATE INDEX IF NOT EXISTS idx_meter_readings_unit_service_date 
    ON billing.meter_readings (unit_id, service_code, reading_date DESC);

