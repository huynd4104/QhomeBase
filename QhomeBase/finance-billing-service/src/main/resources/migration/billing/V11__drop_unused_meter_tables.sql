-- V11: Drop unused meter_readings and meter_charge_links tables
-- These tables were not used in the current implementation:
-- - Meter readings are stored in base-service (data.meter_readings)
-- - Finance-billing-service only receives DTOs via API and creates invoices directly
-- - No code inserts into these tables

-- Drop meter_charge_links first (has FK to meter_readings)
DROP TABLE IF EXISTS billing.meter_charge_links CASCADE;

-- Drop meter_readings table
DROP TABLE IF EXISTS billing.meter_readings CASCADE;

