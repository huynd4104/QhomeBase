-- Add CARD_REJECTED to notification_type enum
DO $$
BEGIN
    -- Check if CARD_REJECTED already exists in the enum
    IF NOT EXISTS (
        SELECT 1 FROM pg_enum 
        WHERE enumlabel = 'CARD_REJECTED' 
        AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'notification_type' AND typnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'content'))
    ) THEN
        ALTER TYPE content.notification_type ADD VALUE 'CARD_REJECTED';
    END IF;
END $$;

