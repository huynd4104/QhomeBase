-- V36: Fix meters service foreign key constraint to reference data.services correctly
-- This fixes the constraint that was created incorrectly in earlier migrations

-- Drop old constraint from meters table if exists
ALTER TABLE data.meters
DROP CONSTRAINT IF EXISTS fk_meters_service;

-- Add correct constraint referencing data.services
ALTER TABLE data.meters
ADD CONSTRAINT fk_meters_service 
FOREIGN KEY (service_id) REFERENCES data.services(id) ON DELETE RESTRICT;

COMMENT ON CONSTRAINT fk_meters_service ON data.meters 
IS 'References data.services (fixed from incorrect schema reference)';


