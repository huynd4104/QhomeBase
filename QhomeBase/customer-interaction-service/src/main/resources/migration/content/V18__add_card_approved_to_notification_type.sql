-- Add CARD_APPROVED to notification_type enum
DO $$
BEGIN
    -- Check if CARD_APPROVED already exists in the enum
    IF NOT EXISTS (
        SELECT 1 FROM pg_enum 
        WHERE enumlabel = 'CARD_APPROVED' 
        AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'notification_type' AND typnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'content'))
    ) THEN
        ALTER TYPE content.notification_type ADD VALUE 'CARD_APPROVED';
    END IF;
END $$;

