-- Create table to track message deletions
CREATE TABLE IF NOT EXISTS chat_service.direct_message_deletions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES chat_service.direct_messages(id) ON DELETE CASCADE,
    deleted_by_user_id UUID NOT NULL,
    delete_type VARCHAR(20) NOT NULL CHECK (delete_type IN ('FOR_ME', 'FOR_EVERYONE')),
    deleted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(message_id, deleted_by_user_id, delete_type)
);

-- Index for faster queries
CREATE INDEX IF NOT EXISTS idx_direct_message_deletions_message_id ON chat_service.direct_message_deletions(message_id);
CREATE INDEX IF NOT EXISTS idx_direct_message_deletions_deleted_by ON chat_service.direct_message_deletions(deleted_by_user_id);
CREATE INDEX IF NOT EXISTS idx_direct_message_deletions_type ON chat_service.direct_message_deletions(delete_type);

-- Comment
COMMENT ON TABLE chat_service.direct_message_deletions IS 'Tracks which users have deleted which messages and how (for me or for everyone)';

