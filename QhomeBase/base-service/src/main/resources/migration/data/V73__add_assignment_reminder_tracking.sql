ALTER TABLE data.meter_reading_assignments
    ADD COLUMN IF NOT EXISTS reminder_last_sent_date DATE;

COMMENT ON COLUMN data.meter_reading_assignments.reminder_last_sent_date
    IS 'Ngày gần nhất đã gửi nhắc nhở tới nhân viên cho phân công này';








