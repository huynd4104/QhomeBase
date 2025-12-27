-- Allow sender_id to be NULL for system messages
ALTER TABLE chat_service.messages 
    ALTER COLUMN sender_id DROP NOT NULL;

-- Add a check constraint to ensure sender_id is NOT NULL for non-system messages
ALTER TABLE chat_service.messages 
    ADD CONSTRAINT check_sender_id_for_non_system_messages 
    CHECK (
        (message_type = 'SYSTEM' AND sender_id IS NULL) OR 
        (message_type != 'SYSTEM' AND sender_id IS NOT NULL)
    );

