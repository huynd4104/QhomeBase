-- Add mime_type column to group_files table
ALTER TABLE chat_service.group_files 
ADD COLUMN IF NOT EXISTS mime_type VARCHAR(100);

-- Update existing records: copy file_type to mime_type if mime_type is null
UPDATE chat_service.group_files 
SET mime_type = file_type 
WHERE mime_type IS NULL AND file_type IS NOT NULL;

