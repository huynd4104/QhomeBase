-- Add view to track meter reading status within assignments
-- This allows knowing which meters have been read vs not read in a specific assignment

CREATE OR REPLACE VIEW data.v_meters_with_reading_status AS
SELECT 
    m.id AS meter_id,
    m.meter_code,
    m.unit_id,
    u.code AS unit_code,
    u.floor,
    u.building_id,
    b.code AS building_code,
    b.name AS building_name,
    m.service_id,
    s.code AS service_code,
    s.name AS service_name,
    m.active,
    m.installed_at,
    mra.id AS assignment_id,
    mra.cycle_id,
    rc.name AS cycle_name,
    mra.start_date AS assignment_start_date,
    mra.end_date AS assignment_end_date,
    mr.id AS reading_id,
    mr.reading_date,
    mr.curr_index,
    mr.verified,
    CASE 
        WHEN mr.id IS NOT NULL THEN 'READ'
        ELSE 'PENDING'
    END AS reading_status,
    mr.reader_id,
    mr.read_at,
    mr.verified_by,
    mr.verified_at
FROM data.meters m
INNER JOIN data.units u ON m.unit_id = u.id
INNER JOIN data.buildings b ON u.building_id = b.id
INNER JOIN svc.services s ON m.service_id = s.id
INNER JOIN data.meter_reading_assignments mra ON (
    mra.building_id = b.id 
    AND mra.service_id = s.id
    AND (
        (mra.unit_ids IS NULL AND mra.floor IS NULL) OR  -- All floors
        (mra.unit_ids IS NULL AND mra.floor = u.floor) OR  -- Specific floor
        (mra.unit_ids IS NOT NULL AND u.id = ANY(mra.unit_ids))  -- Specific units
    )
)
INNER JOIN data.reading_cycles rc ON mra.cycle_id = rc.id
LEFT JOIN data.meter_readings mr ON (
    mr.meter_id = m.id 
    AND mr.assignment_id = mra.id
)
WHERE m.active = TRUE;

COMMENT ON VIEW data.v_meters_with_reading_status IS 'Meters with reading status for each assignment. Shows which meters have been read (READ) vs pending (PENDING) in each assignment.';

-- Function to get meters with reading status for a specific assignment
CREATE OR REPLACE FUNCTION data.get_meters_with_status_for_assignment(
    p_assignment_id UUID
)
RETURNS TABLE (
    meter_id UUID,
    meter_code TEXT,
    unit_id UUID,
    unit_code TEXT,
    floor INTEGER,
    building_id UUID,
    building_code TEXT,
    building_name TEXT,
    service_id UUID,
    service_code TEXT,
    service_name TEXT,
    reading_status TEXT,
    reading_id UUID,
    reading_date DATE,
    curr_index NUMERIC,
    verified BOOLEAN,
    reader_id UUID,
    read_at TIMESTAMPTZ
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        v.meter_id,
        v.meter_code,
        v.unit_id,
        v.unit_code,
        v.floor,
        v.building_id,
        v.building_code,
        v.building_name,
        v.service_id,
        v.service_code,
        v.service_name,
        v.reading_status,
        v.reading_id,
        v.reading_date,
        v.curr_index,
        v.verified,
        v.reader_id,
        v.read_at
    FROM data.v_meters_with_reading_status v
    WHERE v.assignment_id = p_assignment_id
    ORDER BY v.floor, v.unit_code, v.meter_code;
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION data.get_meters_with_status_for_assignment IS 'Get all meters for a specific assignment with their reading status (READ/PENDING)';


