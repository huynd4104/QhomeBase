CREATE TABLE IF NOT EXISTS data.meter_reading_reminders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id UUID NOT NULL REFERENCES data.meter_reading_assignments(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    due_date DATE NOT NULL,
    type VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    acknowledged_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_meter_reading_reminders_user
    ON data.meter_reading_reminders (user_id, created_at DESC);

COMMENT ON TABLE data.meter_reading_reminders IS 'Lưu các thông báo nhắc nhở đo chỉ số cho nhân viên';
COMMENT ON COLUMN data.meter_reading_reminders.type IS 'Loại nhắc nhở (VD: METER_READING_ASSIGNMENT_REMINDER)';








