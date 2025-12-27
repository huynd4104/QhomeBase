-- Replace floor_from and floor_to with single floor column
-- This migration simplifies floor assignment to avoid business logic errors

-- Step 1: Drop views that depend on floor_from and floor_to columns
DROP VIEW IF EXISTS data.v_reading_assignments_status CASCADE;
DROP VIEW IF EXISTS data.v_meter_reading_kanban CASCADE;

-- Step 2: Add new floor column
ALTER TABLE data.meter_reading_assignments
ADD COLUMN floor INTEGER;

-- Step 3: Migrate existing data (if floor_from = floor_to, use that value, otherwise set to NULL)
UPDATE data.meter_reading_assignments
SET floor = CASE 
    WHEN floor_from IS NOT NULL AND floor_to IS NOT NULL AND floor_from = floor_to THEN floor_from
    WHEN floor_from IS NOT NULL AND floor_to IS NULL THEN floor_from
    WHEN floor_from IS NULL AND floor_to IS NOT NULL THEN floor_to
    ELSE NULL
END;

-- Step 4: Drop old columns and constraints
ALTER TABLE data.meter_reading_assignments
DROP CONSTRAINT IF EXISTS ck_floor_range;

DROP INDEX IF EXISTS data.idx_assignments_building_service_floor;

ALTER TABLE data.meter_reading_assignments
DROP COLUMN floor_from,
DROP COLUMN floor_to;

-- Step 5: Create new index for floor column
CREATE INDEX idx_assignments_building_service_floor 
    ON data.meter_reading_assignments(building_id, service_id, floor)
    WHERE floor IS NOT NULL;

COMMENT ON COLUMN data.meter_reading_assignments.floor IS 'Specific floor number for assignment scope (nullable = all floors)';

-- Step 6: Recreate v_reading_assignments_status view with floor column
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

-- Step 7: Drop old function with floor_from and floor_to parameters
DROP FUNCTION IF EXISTS data.get_meters_for_assignment(UUID, UUID, INTEGER, INTEGER);

-- Step 8: Create function with single floor parameter
CREATE OR REPLACE FUNCTION data.get_meters_for_assignment(
    p_building_id UUID,
    p_service_id UUID,
    p_floor INTEGER DEFAULT NULL
)
RETURNS TABLE (
    meter_id UUID,
    unit_id UUID,
    unit_code TEXT,
    floor INTEGER,
    meter_code TEXT,
    installed_at DATE
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        m.id AS meter_id,
        m.unit_id,
        u.code AS unit_code,
        u.floor,
        m.meter_code,
        m.installed_at
    FROM data.meters m
    INNER JOIN data.units u ON m.unit_id = u.id
    WHERE m.service_id = p_service_id
      AND u.building_id = p_building_id
      AND m.active = TRUE
      AND (p_floor IS NULL OR u.floor = p_floor)
    ORDER BY u.floor, u.code;
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION data.get_meters_for_assignment IS 'Get all active meters for a building/service on specific floor (NULL = all floors)';

-- Step 9: Drop old function with floor_from and floor_to parameters
DROP FUNCTION IF EXISTS data.validate_assignment_overlap(UUID, UUID, UUID, INTEGER, INTEGER, UUID);

-- Step 10: Create overlap validation function with single floor parameter
CREATE OR REPLACE FUNCTION data.validate_assignment_overlap(
    p_cycle_id UUID,
    p_building_id UUID,
    p_service_id UUID,
    p_floor INTEGER,
    p_exclude_assignment_id UUID DEFAULT NULL
)
RETURNS BOOLEAN AS $$
DECLARE
    v_overlap_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_overlap_count
    FROM data.meter_reading_assignments
    WHERE cycle_id = p_cycle_id
      AND building_id = p_building_id
      AND service_id = p_service_id
      AND (p_exclude_assignment_id IS NULL OR id != p_exclude_assignment_id)
      AND completed_at IS NULL
      AND (
          (floor IS NULL AND p_floor IS NULL)
          OR (floor = p_floor)
      );
    
    RETURN v_overlap_count = 0;
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION data.validate_assignment_overlap IS 'Check if assignment floor overlaps with existing assignments in the same cycle/building/service';

