-- Add views to track meter reading status
-- Multiple views for different use cases

-- View 1: Simple meter reading status by assignment
-- Shows meter status (READ/PENDING) for a specific assignment
CREATE OR REPLACE VIEW data.v_meter_reading_status AS
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
    mra.id AS assignment_id,
    mra.cycle_id,
    rc.name AS cycle_name,
    mra.status AS assignment_status,
    mra.start_date AS assignment_start_date,
    mra.end_date AS assignment_end_date,
    CASE 
        WHEN mr.id IS NOT NULL THEN 'READ'
        ELSE 'PENDING'
    END AS reading_status,
    mr.id AS reading_id,
    mr.reading_date,
    mr.prev_index,
    mr.curr_index,
    mr.consumption,
    mr.verified,
    mr.reader_id,
    mr.read_at,
    mr.verified_by,
    mr.verified_at,
    CASE 
        WHEN mr.id IS NOT NULL AND mr.verified = TRUE THEN 'VERIFIED'
        WHEN mr.id IS NOT NULL AND mr.verified = FALSE THEN 'READ'
        WHEN mr.id IS NULL AND mra.end_date < CURRENT_DATE THEN 'OVERDUE'
        WHEN mr.id IS NULL AND mra.start_date <= CURRENT_DATE AND mra.end_date >= CURRENT_DATE THEN 'PENDING'
        WHEN mr.id IS NULL THEN 'PENDING'
        ELSE 'UNKNOWN'
    END AS detailed_status
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
    AND mra.status NOT IN ('CANCELLED')
)
INNER JOIN data.reading_cycles rc ON mra.cycle_id = rc.id
LEFT JOIN data.meter_readings mr ON (
    mr.meter_id = m.id 
    AND mr.assignment_id = mra.id
)
WHERE m.active = TRUE;

COMMENT ON VIEW data.v_meter_reading_status IS 'Meter reading status by assignment. Shows READ/PENDING/VERIFIED/OVERDUE status for each meter in each assignment.';

-- View 2: Meter reading status summary by cycle
-- Shows aggregated reading status for all meters in a cycle
CREATE OR REPLACE VIEW data.v_meter_reading_status_by_cycle AS
SELECT 
    rc.id AS cycle_id,
    rc.name AS cycle_name,
    rc.period_from,
    rc.period_to,
    rc.status AS cycle_status,
    m.service_id,
    s.code AS service_code,
    s.name AS service_name,
    b.id AS building_id,
    b.code AS building_code,
    b.name AS building_name,
    COUNT(DISTINCT m.id) AS total_meters,
    COUNT(DISTINCT CASE WHEN mr.id IS NOT NULL THEN m.id END) AS read_meters,
    COUNT(DISTINCT CASE WHEN mr.id IS NULL THEN m.id END) AS pending_meters,
    COUNT(DISTINCT CASE WHEN mr.id IS NOT NULL AND mr.verified = TRUE THEN m.id END) AS verified_meters,
    COUNT(DISTINCT CASE WHEN mr.id IS NULL AND mra.end_date < CURRENT_DATE THEN m.id END) AS overdue_meters,
    ROUND(
        CASE 
            WHEN COUNT(DISTINCT m.id) > 0 
            THEN (COUNT(DISTINCT CASE WHEN mr.id IS NOT NULL THEN m.id END)::NUMERIC / COUNT(DISTINCT m.id)::NUMERIC) * 100
            ELSE 0
        END, 2
    ) AS reading_progress_percentage
FROM data.reading_cycles rc
INNER JOIN data.meter_reading_assignments mra ON mra.cycle_id = rc.id
INNER JOIN data.buildings b ON mra.building_id = b.id
INNER JOIN svc.services s ON mra.service_id = s.id
INNER JOIN data.meters m ON (
    m.service_id = s.id
    AND m.unit_id IN (
        SELECT u.id FROM data.units u 
        WHERE u.building_id = b.id
        AND (
            (mra.unit_ids IS NULL AND mra.floor IS NULL) OR
            (mra.unit_ids IS NULL AND mra.floor = u.floor) OR
            (mra.unit_ids IS NOT NULL AND u.id = ANY(mra.unit_ids))
        )
    )
)
LEFT JOIN data.meter_readings mr ON (
    mr.meter_id = m.id 
    AND mr.assignment_id = mra.id
)
WHERE m.active = TRUE
  AND mra.status NOT IN ('CANCELLED')
GROUP BY 
    rc.id, rc.name, rc.period_from, rc.period_to, rc.status,
    m.service_id, s.code, s.name,
    b.id, b.code, b.name;

COMMENT ON VIEW data.v_meter_reading_status_by_cycle IS 'Aggregated meter reading status by cycle. Shows total, read, pending, verified, and overdue meters for each cycle/building/service combination.';

-- View 3: Latest meter reading status
-- Shows the latest reading status for each meter (regardless of assignment)
CREATE OR REPLACE VIEW data.v_meter_latest_reading_status AS
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
    latest_reading.reading_id,
    latest_reading.reading_date AS last_reading_date,
    latest_reading.curr_index AS last_index,
    latest_reading.verified AS last_verified,
    latest_reading.reader_id AS last_reader_id,
    latest_reading.read_at AS last_read_at,
    latest_reading.assignment_id AS last_assignment_id,
    latest_reading.cycle_id AS last_cycle_id,
    latest_reading.cycle_name AS last_cycle_name,
    CASE 
        WHEN latest_reading.reading_id IS NOT NULL THEN 'READ'
        ELSE 'NEVER_READ'
    END AS reading_status
FROM data.meters m
INNER JOIN data.units u ON m.unit_id = u.id
INNER JOIN data.buildings b ON u.building_id = b.id
INNER JOIN svc.services s ON m.service_id = s.id
LEFT JOIN LATERAL (
    SELECT 
        mr.id AS reading_id,
        mr.reading_date,
        mr.curr_index,
        mr.verified,
        mr.reader_id,
        mr.read_at,
        mr.assignment_id,
        mra.cycle_id,
        rc.name AS cycle_name
    FROM data.meter_readings mr
    LEFT JOIN data.meter_reading_assignments mra ON mr.assignment_id = mra.id
    LEFT JOIN data.reading_cycles rc ON mra.cycle_id = rc.id
    WHERE mr.meter_id = m.id
    ORDER BY mr.reading_date DESC, mr.read_at DESC
    LIMIT 1
) AS latest_reading ON TRUE
WHERE m.active = TRUE;

COMMENT ON VIEW data.v_meter_latest_reading_status IS 'Latest reading status for each meter. Shows the most recent reading for each meter regardless of assignment.';

-- Function: Get meter reading status for a specific assignment
CREATE OR REPLACE FUNCTION data.get_meter_reading_status_by_assignment(
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
    detailed_status TEXT,
    reading_id UUID,
    reading_date DATE,
    prev_index NUMERIC,
    curr_index NUMERIC,
    consumption NUMERIC,
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
        v.detailed_status,
        v.reading_id,
        v.reading_date,
        v.prev_index,
        v.curr_index,
        v.consumption,
        v.verified,
        v.reader_id,
        v.read_at
    FROM data.v_meter_reading_status v
    WHERE v.assignment_id = p_assignment_id
    ORDER BY v.floor, v.unit_code, v.meter_code;
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION data.get_meter_reading_status_by_assignment IS 'Get meter reading status for a specific assignment with detailed status (READ/PENDING/VERIFIED/OVERDUE)';


