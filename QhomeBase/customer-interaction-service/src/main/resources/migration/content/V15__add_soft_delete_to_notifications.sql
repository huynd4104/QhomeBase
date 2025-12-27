-- Add soft delete columns to notifications table
ALTER TABLE content.notifications 
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS deleted_by UUID;

-- Add index for soft delete queries
CREATE INDEX IF NOT EXISTS ix_notifications_deleted_at ON content.notifications(deleted_at) WHERE deleted_at IS NOT NULL;

-- Add comment
COMMENT ON COLUMN content.notifications.deleted_at IS 'Timestamp when notification was soft deleted (NULL = active)';
COMMENT ON COLUMN content.notifications.deleted_by IS 'User ID who deleted the notification';














