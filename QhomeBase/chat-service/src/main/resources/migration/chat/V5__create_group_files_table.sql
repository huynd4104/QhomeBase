-- Create group_files table to store file metadata
CREATE TABLE IF NOT EXISTS chat_service.group_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL REFERENCES chat_service.groups(id) ON DELETE CASCADE,
    message_id UUID NOT NULL REFERENCES chat_service.messages(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(100), -- mimeType
    file_url VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_group_files_group FOREIGN KEY (group_id) REFERENCES chat_service.groups(id),
    CONSTRAINT fk_group_files_message FOREIGN KEY (message_id) REFERENCES chat_service.messages(id)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_group_files_group_id ON chat_service.group_files(group_id);
CREATE INDEX IF NOT EXISTS idx_group_files_created_at ON chat_service.group_files(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_group_files_sender_id ON chat_service.group_files(sender_id);
CREATE INDEX IF NOT EXISTS idx_group_files_message_id ON chat_service.group_files(message_id);

-- Add comment
COMMENT ON TABLE chat_service.group_files IS 'Stores metadata of all files sent in group chats';

