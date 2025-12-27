-- Add mime_type column to messages table
ALTER TABLE chat_service.messages 
ADD COLUMN IF NOT EXISTS mime_type VARCHAR(100);

-- Add index for mime_type if needed (optional)
-- CREATE INDEX IF NOT EXISTS idx_messages_mime_type ON chat_service.messages(mime_type);

