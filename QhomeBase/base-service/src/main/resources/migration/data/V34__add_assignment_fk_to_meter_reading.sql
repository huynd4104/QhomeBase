
ALTER TABLE data.meter_readings
    ADD COLUMN IF NOT EXISTS assignment_id UUID;


ALTER TABLE data.meter_readings
    ADD CONSTRAINT fk_meter_reading_assignment
        FOREIGN KEY (assignment_id) REFERENCES data.meter_reading_assignments(id) ON DELETE CASCADE;


CREATE INDEX IF NOT EXISTS idx_meter_readings_assignment_id
    ON data.meter_readings(assignment_id);