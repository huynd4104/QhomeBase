-- Add reminder tracking fields to invoices table
ALTER TABLE billing.invoices
ADD COLUMN IF NOT EXISTS reminder_count INTEGER NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS last_reminder_at TIMESTAMPTZ;

-- Add index for efficient querying of invoices that need reminders
CREATE INDEX IF NOT EXISTS idx_invoices_reminder_status 
ON billing.invoices(status, reminder_count, last_reminder_at, issued_at)
WHERE status = 'PUBLISHED';

COMMENT ON COLUMN billing.invoices.reminder_count IS 'Số lần đã gửi reminder cho hóa đơn này (tối đa 7 lần)';
COMMENT ON COLUMN billing.invoices.last_reminder_at IS 'Thời điểm gửi reminder lần cuối cùng';

