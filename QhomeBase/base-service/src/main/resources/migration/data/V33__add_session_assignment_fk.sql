ALTER TABLE data.meter_reading_sessions
    ADD COLUMN IF NOT EXISTS assignment_id UUID;

ALTER TABLE data.meter_reading_sessions
    ADD CONSTRAINT fk_session_assignment
        FOREIGN KEY (assignment_id) REFERENCES data.meter_reading_assignments(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_sessions_active ON data.meter_reading_sessions(reader_id, assignment_id) WHERE completed_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_sessions_completed ON data.meter_reading_sessions(assignment_id, completed_at);
