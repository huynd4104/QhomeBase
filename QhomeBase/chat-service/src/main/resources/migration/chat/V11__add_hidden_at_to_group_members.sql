-- Add hidden_at column to group_members for soft delete functionality
-- When a user "deletes" a group, it will be hidden (not removed) and messages/files will be deleted
ALTER TABLE chat_service.group_members
ADD COLUMN IF NOT EXISTS hidden_at TIMESTAMP WITH TIME ZONE;

-- Create index for hidden_at queries
CREATE INDEX IF NOT EXISTS idx_group_members_hidden_at 
ON chat_service.group_members(hidden_at) 
WHERE hidden_at IS NOT NULL;

-- Add comment
COMMENT ON COLUMN chat_service.group_members.hidden_at IS 'Timestamp when user hid/deleted the group. NULL means group is visible. When a new message arrives, this should be set to NULL to unhide the group.';
