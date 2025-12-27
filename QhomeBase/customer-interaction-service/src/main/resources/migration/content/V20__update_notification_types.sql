-- Update notification_type enum to add new types: INFO, ELECTRICITY, WATER
-- Keep existing types for backward compatibility

DO $$
BEGIN
    -- Add INFO type (if not exists)
    IF NOT EXISTS (
        SELECT 1 FROM pg_enum 
        WHERE enumlabel = 'INFO' 
        AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'notification_type' AND typnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'content'))
    ) THEN
        ALTER TYPE content.notification_type ADD VALUE 'INFO';
    END IF;

    -- Add ELECTRICITY type (if not exists)
    IF NOT EXISTS (
        SELECT 1 FROM pg_enum 
        WHERE enumlabel = 'ELECTRICITY' 
        AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'notification_type' AND typnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'content'))
    ) THEN
        ALTER TYPE content.notification_type ADD VALUE 'ELECTRICITY';
    END IF;

    -- Add WATER type (if not exists)
    IF NOT EXISTS (
        SELECT 1 FROM pg_enum 
        WHERE enumlabel = 'WATER' 
        AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'notification_type' AND typnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'content'))
    ) THEN
        ALTER TYPE content.notification_type ADD VALUE 'WATER';
    END IF;
END $$;

-- Update comment for notification_type
COMMENT ON TYPE content.notification_type IS 'Loại thông báo: INFO (Thông tin), REQUEST (Yêu cầu), BILL (Hóa Đơn), CONTRACT (Hợp đồng), ELECTRICITY (Tiền điện), WATER (Tiền nước), SYSTEM (Hệ thống)';

