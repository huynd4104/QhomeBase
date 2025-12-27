-- Add column to track when third reminder was sent
ALTER TABLE files.contracts
ADD COLUMN IF NOT EXISTS third_reminder_sent_at TIMESTAMPTZ;

COMMENT ON COLUMN files.contracts.third_reminder_sent_at IS 'Timestamp when the third (final) renewal reminder was sent. Used to track 24-hour window for auto-cancellation.';
