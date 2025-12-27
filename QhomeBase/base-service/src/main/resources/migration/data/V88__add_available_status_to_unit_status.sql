-- V88: Add AVAILABLE status to unit_status enum
-- AVAILABLE means the unit is available for rent (có thể cho thuê)

-- Add AVAILABLE to the unit_status enum
DO $$
BEGIN
    -- Check if AVAILABLE already exists in the enum
    IF NOT EXISTS (
        SELECT 1 
        FROM pg_enum 
        WHERE enumlabel = 'AVAILABLE' 
        AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'unit_status')
    ) THEN
        -- Add AVAILABLE before VACANT to maintain logical order
        ALTER TYPE data.unit_status ADD VALUE 'AVAILABLE' BEFORE 'VACANT';
        RAISE NOTICE 'Added AVAILABLE to unit_status enum';
    ELSE
        RAISE NOTICE 'AVAILABLE already exists in unit_status enum';
    END IF;
END $$;

COMMENT ON TYPE data.unit_status IS 'Trạng thái căn hộ: ACTIVE (đang hoạt động), AVAILABLE (có thể cho thuê), VACANT (trống), MAINTENANCE (bảo trì), INACTIVE (không hoạt động)';

