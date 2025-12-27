-- V27: Add last_dismissed_reminder_count column to contracts table
-- This column tracks which reminder the user has dismissed
-- User will only see reminders where reminderCount > lastDismissedReminderCount

ALTER TABLE files.contracts 
ADD COLUMN IF NOT EXISTS last_dismissed_reminder_count INTEGER DEFAULT 0;

COMMENT ON COLUMN files.contracts.last_dismissed_reminder_count IS 
'Track which reminder count user has dismissed: 0=not dismissed, 1=dismissed reminder 1, 2=dismissed reminder 2. Only show reminder if reminderCount > lastDismissedReminderCount';
