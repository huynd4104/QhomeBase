-- V29: Fix V28 migration - properly handle views before dropping due_date

-- Step 1: Drop views that depend on due_date column
DROP VIEW IF EXISTS data.v_reading_assignments_status CASCADE;
DROP VIEW IF EXISTS data.v_meter_reading_kanban CASCADE;

-- Step 2: Add new columns if they don't exist
ALTER TABLE data.meter_reading_assignments
ADD COLUMN IF NOT EXISTS start_date DATE,
ADD COLUMN IF NOT EXISTS end_date DATE;

-- Step 3: Migrate existing data from due_date to end_date
UPDATE data.meter_reading_assignments
SET end_date = due_date
WHERE due_date IS NOT NULL AND end_date IS NULL;

-- Step 4: Set start_date from cycle period_from if null
UPDATE data.meter_reading_assignments ma
SET start_date = rc.period_from
FROM data.reading_cycles rc
WHERE ma.cycle_id = rc.id AND ma.start_date IS NULL;

-- Step 5: Drop old due_date column
ALTER TABLE data.meter_reading_assignments
DROP COLUMN IF EXISTS due_date;

-- Step 6: Add constraints
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'ck_assignment_date_range'
    ) THEN
        ALTER TABLE data.meter_reading_assignments
        ADD CONSTRAINT ck_assignment_date_range 
            CHECK (start_date IS NULL OR end_date IS NULL OR start_date <= end_date);
    END IF;
END $$;

-- Step 7: Add indexes
CREATE INDEX IF NOT EXISTS idx_assignments_time_range 
    ON data.meter_reading_assignments(building_id, service_id, start_date, end_date)
    WHERE start_date IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_assignments_active 
    ON data.meter_reading_assignments(start_date, end_date)
    WHERE completed_at IS NULL;

-- Step 8: Add comments
COMMENT ON COLUMN data.meter_reading_assignments.start_date IS 'Expected start date for this assignment (default = cycle period_from)';
COMMENT ON COLUMN data.meter_reading_assignments.end_date IS 'Expected completion date / deadline (default = cycle period_to)';

-- Step 9: Recreate v_reading_assignments_status view with new columns
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
    a.floor_from,
    a.floor_to,
    CASE 
        WHEN a.completed_at IS NOT NULL THEN 'COMPLETED'
        WHEN a.end_date < CURRENT_DATE THEN 'OVERDUE'
        WHEN a.start_date <= CURRENT_DATE AND a.end_date >= CURRENT_DATE THEN 'IN_PROGRESS'
        ELSE 'PENDING'
    END AS status,
    CASE 
        WHEN a.completed_at IS NOT NULL THEN 100
        WHEN a.end_date < CURRENT_DATE THEN 0
        WHEN a.start_date IS NULL OR a.end_date IS NULL THEN 0
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

COMMENT ON VIEW data.v_reading_assignments_status IS 'Assignment status with progress calculation based on start_date and end_date';

