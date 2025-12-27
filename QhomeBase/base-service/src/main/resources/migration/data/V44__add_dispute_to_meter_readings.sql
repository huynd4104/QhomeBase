-- Add dispute handling to meter readings
-- Flow: Reading → Auto Invoice → Dispute (if any) → Verify
-- Verify is only needed when there is a dispute

-- Step 1: Add disputed column
ALTER TABLE data.meter_readings
ADD COLUMN IF NOT EXISTS disputed BOOLEAN DEFAULT FALSE;

-- Step 2: Add dispute fields
ALTER TABLE data.meter_readings
ADD COLUMN IF NOT EXISTS disputed_by UUID,
ADD COLUMN IF NOT EXISTS disputed_at TIMESTAMPTZ,
ADD COLUMN IF NOT EXISTS dispute_reason TEXT;

-- Step 3: Add index for dispute queries
CREATE INDEX IF NOT EXISTS idx_readings_disputed 
    ON data.meter_readings(disputed, verified) 
    WHERE disputed = TRUE;

-- Step 4: Add check constraint
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'ck_dispute_requires_reason'
    ) THEN
        ALTER TABLE data.meter_readings
        ADD CONSTRAINT ck_dispute_requires_reason
            CHECK (
                (disputed = FALSE) OR 
                (disputed = TRUE AND dispute_reason IS NOT NULL)
            );
    END IF;
END$$;

COMMENT ON COLUMN data.meter_readings.disputed IS 'Whether this reading has been disputed by resident. If true, verification is required before invoice can be adjusted.';
COMMENT ON COLUMN data.meter_readings.disputed_by IS 'Resident/user ID who disputed this reading';
COMMENT ON COLUMN data.meter_readings.disputed_at IS 'When the dispute was raised';
COMMENT ON COLUMN data.meter_readings.dispute_reason IS 'Reason for dispute (required if disputed = true)';
COMMENT ON COLUMN data.meter_readings.verified IS 'Whether this reading has been verified by admin/accountant. Only needed when disputed = true.';

-- Step 5: Views will be removed in V45 - keep logic simple in service layer
-- Status checking: LEFT JOIN meter_readings - if exists = READ, if not = PENDING

