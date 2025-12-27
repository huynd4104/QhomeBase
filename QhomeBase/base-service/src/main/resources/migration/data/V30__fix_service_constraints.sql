-- Fix foreign key constraints to reference correct schema (data.services instead of svc.services)

-- Drop old constraints from V23 that reference wrong schema
ALTER TABLE data.meter_reading_assignments
DROP CONSTRAINT IF EXISTS fk_assignment_service;

ALTER TABLE data.meter_reading_sessions
DROP CONSTRAINT IF EXISTS fk_session_service;

-- Drop duplicate constraint from V27 if exists
ALTER TABLE data.meter_reading_assignments
DROP CONSTRAINT IF EXISTS fk_assignments_service;

ALTER TABLE data.meter_reading_sessions
DROP CONSTRAINT IF EXISTS fk_sessions_service;

-- Add correct constraints referencing data.services
ALTER TABLE data.meter_reading_assignments
ADD CONSTRAINT fk_assignment_service 
FOREIGN KEY (service_id) REFERENCES data.services(id) ON DELETE RESTRICT;

ALTER TABLE data.meter_reading_sessions
ADD CONSTRAINT fk_session_service 
FOREIGN KEY (service_id) REFERENCES data.services(id) ON DELETE RESTRICT;

COMMENT ON CONSTRAINT fk_assignment_service ON data.meter_reading_assignments 
IS 'References data.services (fixed from incorrect svc.services)';

COMMENT ON CONSTRAINT fk_session_service ON data.meter_reading_sessions 
IS 'References data.services (fixed from incorrect svc.services)';

