-- Add status column to meter_reading_assignments
-- Status: PENDING, IN_PROGRESS, COMPLETED, CANCELLED, OVERDUE

-- Step 1: Add status column (nullable initially for data migration)
ALTER TABLE data.meter_reading_assignments
ADD COLUMN IF NOT EXISTS status TEXT;

-- Step 2: Set initial status based on existing data
UPDATE data.meter_reading_assignments
SET status = CASE
    WHEN completed_at IS NOT NULL THEN 'COMPLETED'
    WHEN end_date IS NOT NULL AND end_date < CURRENT_DATE THEN 'OVERDUE'
    WHEN start_date IS NOT NULL AND start_date <= CURRENT_DATE AND end_date >= CURRENT_DATE THEN 'IN_PROGRESS'
    ELSE 'PENDING'
END
WHERE status IS NULL;

-- Step 3: Make status NOT NULL after data migration
ALTER TABLE data.meter_reading_assignments
ALTER COLUMN status SET NOT NULL;

-- Step 4: Set default value
ALTER TABLE data.meter_reading_assignments
ALTER COLUMN status SET DEFAULT 'PENDING';

-- Step 5: Add check constraint
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'ck_assignment_status'
    ) THEN
        ALTER TABLE data.meter_reading_assignments
        ADD CONSTRAINT ck_assignment_status
            CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'OVERDUE'));
    END IF;
END$$;

-- Step 6: Create index for status queries
CREATE INDEX IF NOT EXISTS idx_assignments_status 
    ON data.meter_reading_assignments(status, completed_at);

-- Step 7: Update v_reading_assignments_status view to use direct status
DROP VIEW IF EXISTS data.v_reading_assignments_status CASCADE;

CREATE OR REPLACE VIEW data.v_reading_assignments_status AS
SELECT 
    a.id AS assignment_id,
    a.cycle_id,
    rc.name AS cycle_name,
    a.building_id,
    b.code AS building_code,
    b.name AS building_name,
    a.service_id,
    a.assigned_to,
    a.assigned_by,
    a.assigned_at,
    a.start_date,
    a.end_date,
    a.completed_at,
    a.floor,
    a.status,
    CASE 
        WHEN a.completed_at IS NOT NULL THEN 100
        WHEN a.end_date IS NULL OR a.start_date IS NULL THEN 0
        WHEN a.end_date < CURRENT_DATE THEN 0
        ELSE LEAST(100, GREATEST(0, 
            ROUND(
                ((CURRENT_DATE - a.start_date)::NUMERIC / 
                 NULLIF((a.end_date - a.start_date)::NUMERIC, 0)) * 100
            )::INTEGER
        ))
    END AS progress_percentage
FROM data.meter_reading_assignments a
INNER JOIN data.reading_cycles rc ON a.cycle_id = rc.id
LEFT JOIN data.buildings b ON a.building_id = b.id;

COMMENT ON VIEW data.v_reading_assignments_status IS 'Assignment status with progress calculation (using direct status column)';
COMMENT ON COLUMN data.meter_reading_assignments.status IS 'Assignment status: PENDING, IN_PROGRESS, COMPLETED, CANCELLED, OVERDUE';

