-- V27: Add target_resident_id to notifications table
-- This allows notifications to be sent to a specific resident instead of all residents in a building

ALTER TABLE content.notifications
    ADD COLUMN IF NOT EXISTS target_resident_id UUID;

CREATE INDEX IF NOT EXISTS ix_notifications_target_resident 
    ON content.notifications(target_resident_id) 
    WHERE target_resident_id IS NOT NULL;

COMMENT ON COLUMN content.notifications.target_resident_id IS 'Resident ID nhận thông báo (EXTERNAL only): NULL = all residents in building or all buildings, UUID = resident cụ thể';

