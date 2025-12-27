-- Create notification_views table to track individual user reads

-- Create viewer_type enum if it doesn't exist
DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type t WHERE t.typname='viewer_type' AND t.typnamespace=to_regnamespace('content')) THEN
            EXECUTE 'CREATE TYPE content.viewer_type AS ENUM (''RESIDENT'',''USER'')';
        END IF;
    END$$;

CREATE TABLE IF NOT EXISTS content.notification_views (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID NOT NULL,
    viewer_type content.viewer_type NOT NULL DEFAULT 'RESIDENT',
    resident_id UUID,
    user_id UUID,
    read_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_notification_views_notification FOREIGN KEY (notification_id) 
        REFERENCES content.notifications(id) ON DELETE CASCADE,
    CONSTRAINT chk_notification_views_xor CHECK (
        (resident_id IS NOT NULL AND user_id IS NULL) OR
        (resident_id IS NULL AND user_id IS NOT NULL)
    )
);

-- Create unique indexes to prevent duplicate views
CREATE UNIQUE INDEX IF NOT EXISTS uq_notification_views_resident 
    ON content.notification_views (notification_id, resident_id) 
    WHERE resident_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_notification_views_user 
    ON content.notification_views (notification_id, user_id) 
    WHERE user_id IS NOT NULL;

-- Create indexes for efficient lookups
CREATE INDEX IF NOT EXISTS ix_notification_views_notification 
    ON content.notification_views (notification_id, read_at DESC);

CREATE INDEX IF NOT EXISTS ix_notification_views_resident 
    ON content.notification_views (resident_id, notification_id) 
    WHERE resident_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_notification_views_user 
    ON content.notification_views (user_id, notification_id) 
    WHERE user_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_notification_views_read_at 
    ON content.notification_views (read_at DESC);

-- Drop indexes related to status (no longer needed)
DROP INDEX IF EXISTS content.ix_notifications_status;
DROP INDEX IF EXISTS content.ix_notifications_unread;

-- Drop constraint related to read_at
ALTER TABLE content.notifications 
    DROP CONSTRAINT IF EXISTS chk_notification_read_at;

-- Drop status and read_at columns from notifications table
ALTER TABLE content.notifications 
    DROP COLUMN IF EXISTS status CASCADE,
    DROP COLUMN IF EXISTS read_at CASCADE;

-- Update comments
COMMENT ON TABLE content.notification_views IS 'Bảng track từng user đã đọc notification hay chưa';
COMMENT ON COLUMN content.notification_views.viewer_type IS 'Loại viewer: RESIDENT (resident), USER (staff)';
COMMENT ON COLUMN content.notification_views.resident_id IS 'Resident ID đã đọc (EXTERNAL notifications)';
COMMENT ON COLUMN content.notification_views.user_id IS 'User ID đã đọc (INTERNAL notifications)';
COMMENT ON COLUMN content.notification_views.read_at IS 'Thời gian đọc notification';

COMMENT ON TABLE content.notifications IS 'Bảng lưu trữ thông báo - mỗi notification có thể được nhiều user đọc';
COMMENT ON COLUMN content.notifications.type IS 'Loại thông báo: NEWS, REQUEST, BILL, CONTRACT, METER_READING, SYSTEM';
COMMENT ON COLUMN content.notifications.scope IS 'Phạm vi: INTERNAL (nội bộ - staff), EXTERNAL (bên ngoài - residents)';
COMMENT ON COLUMN content.notifications.target_role IS 'Role nhận thông báo (INTERNAL only): ALL, admin, technician, supporter, accountant';
COMMENT ON COLUMN content.notifications.target_building_id IS 'Building ID nhận thông báo (EXTERNAL only): NULL = ALL buildings, UUID = building cụ thể';



