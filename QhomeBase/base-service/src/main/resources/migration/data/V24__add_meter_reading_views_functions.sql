-- V24: Add views and functions to support meter reading operations

-- View: Reading cycles with progress
CREATE OR REPLACE VIEW data.v_reading_cycles_progress AS
SELECT 
    rc.id,
    rc.name,
    rc.period_from,
    rc.period_to,
    rc.status,
    rc.description,
    COUNT(DISTINCT mra.id) AS assignments_count,
    COUNT(DISTINCT CASE WHEN mra.completed_at IS NOT NULL THEN mra.id END) AS assignments_completed,
    COUNT(DISTINCT mrs.id) AS sessions_count,
    COUNT(DISTINCT CASE WHEN mrs.status = 'COMPLETED' THEN mrs.id END) AS sessions_completed,
    COUNT(DISTINCT mr.id) AS readings_count,
    COUNT(DISTINCT CASE WHEN mr.verified THEN mr.id END) AS readings_verified,
    rc.created_by,
    rc.created_at
FROM data.reading_cycles rc
LEFT JOIN data.meter_reading_assignments mra ON mra.cycle_id = rc.id
LEFT JOIN data.meter_reading_sessions mrs ON mrs.cycle_id = rc.id
LEFT JOIN data.meter_readings mr ON mr.session_id = mrs.id
GROUP BY rc.id, rc.name, rc.period_from, rc.period_to, rc.status, 
         rc.description, rc.created_by, rc.created_at;

-- View: Meter readings with details
CREATE OR REPLACE VIEW data.v_meter_readings_detail AS
SELECT 
    mr.id,
    mr.meter_id,
    m.meter_code,
    m.unit_id,
    u.code AS unit_code,
    u.building_id,
    b.code AS building_code,
    m.service_id,
    s.code AS service_code,
    s.name AS service_name,
    mr.reading_date,
    mr.prev_index,
    mr.curr_index,
    mr.consumption,
    mr.photo_file_id,
    mr.note,
    mr.reader_id,
    mr.read_at,
    mr.verified,
    mr.verified_by,
    mr.verified_at,
    mr.session_id
FROM data.meter_readings mr
JOIN data.meters m ON mr.meter_id = m.id
JOIN data.units u ON m.unit_id = u.id
JOIN data.buildings b ON u.building_id = b.id
JOIN svc.services s ON m.service_id = s.id;

-- View: Reading sessions with progress
CREATE OR REPLACE VIEW data.v_reading_sessions_progress AS
SELECT 
    mrs.id,
    mrs.cycle_id,
    rc.name AS cycle_name,
    mrs.building_id,
    b.code AS building_code,
    mrs.service_id,
    s.code AS service_code,
    s.name AS service_name,
    mrs.reader_id,
    mrs.started_at,
    mrs.completed_at,
    mrs.status,
    mrs.units_read,
    COUNT(mr.id) AS readings_count,
    COUNT(CASE WHEN mr.verified THEN 1 END) AS verified_count
FROM data.meter_reading_sessions mrs
LEFT JOIN data.reading_cycles rc ON mrs.cycle_id = rc.id
LEFT JOIN data.buildings b ON mrs.building_id = b.id
LEFT JOIN svc.services s ON mrs.service_id = s.id
LEFT JOIN data.meter_readings mr ON mr.session_id = mrs.id
GROUP BY mrs.id, rc.id, rc.name, b.code, s.code, s.name, mrs.cycle_id, mrs.building_id, mrs.service_id, 
         mrs.reader_id, mrs.started_at, mrs.completed_at, mrs.status, mrs.units_read;

-- View: Reading assignments status
CREATE OR REPLACE VIEW data.v_reading_assignments_status AS
SELECT 
    mra.id,
    mra.cycle_id,
    rc.name AS cycle_name,
    mra.building_id,
    b.code AS building_code,
    mra.service_id,
    s.code AS service_code,
    s.name AS service_name,
    mra.assigned_to,
    mra.assigned_by,
    mra.assigned_at,
    mra.due_date,
    mra.completed_at,
    CASE 
        WHEN mra.completed_at IS NOT NULL THEN 'COMPLETED'
        WHEN mra.due_date < CURRENT_DATE THEN 'OVERDUE'
        ELSE 'PENDING'
    END AS status,
    COUNT(mrs.id) AS sessions_count,
    SUM(mrs.units_read) AS total_units_read
FROM data.meter_reading_assignments mra
LEFT JOIN data.reading_cycles rc ON mra.cycle_id = rc.id
LEFT JOIN data.buildings b ON mra.building_id = b.id
LEFT JOIN svc.services s ON mra.service_id = s.id
LEFT JOIN data.meter_reading_sessions mrs 
    ON mrs.cycle_id = mra.cycle_id
    AND mrs.building_id IS NOT DISTINCT FROM mra.building_id
    AND mrs.service_id = mra.service_id
    AND mrs.reader_id = mra.assigned_to
GROUP BY mra.id, rc.id, rc.name, b.code, s.code, s.name, mra.cycle_id, mra.building_id, mra.service_id,
         mra.assigned_to, mra.assigned_by, mra.assigned_at, mra.due_date, mra.completed_at;

-- View: Meters pending reading
CREATE OR REPLACE VIEW data.v_meters_pending_reading AS
SELECT 
    m.id AS meter_id,
    m.meter_code,
    m.unit_id,
    u.code AS unit_code,
    u.building_id,
    b.code AS building_code,
    m.service_id,
    s.code AS service_code,
    s.name AS service_name,
    m.active,
    m.installed_at,
    (
        SELECT mr.reading_date
        FROM data.meter_readings mr
        WHERE mr.meter_id = m.id
        ORDER BY mr.reading_date DESC
        LIMIT 1
    ) AS last_reading_date,
    (
        SELECT mr.curr_index
        FROM data.meter_readings mr
        WHERE mr.meter_id = m.id
        ORDER BY mr.reading_date DESC
        LIMIT 1
    ) AS last_index
FROM data.meters m
JOIN data.units u ON m.unit_id = u.id
JOIN data.buildings b ON u.building_id = b.id
JOIN svc.services s ON m.service_id = s.id
WHERE m.active = true
  AND m.removed_at IS NULL;

-- Function: Get last meter reading for a meter
CREATE OR REPLACE FUNCTION data.get_last_meter_reading(
    p_meter_id UUID,
    p_before_date DATE DEFAULT CURRENT_DATE
)
RETURNS TABLE (
    reading_id UUID,
    reading_date DATE,
    curr_index NUMERIC,
    photo_file_id UUID
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        mr.id,
        mr.reading_date,
        mr.curr_index,
        mr.photo_file_id
    FROM data.meter_readings mr
    WHERE mr.meter_id = p_meter_id
      AND mr.reading_date < p_before_date
    ORDER BY mr.reading_date DESC
    LIMIT 1;
END;
$$ LANGUAGE plpgsql;

-- Function: Calculate total consumption for a meter in a period
CREATE OR REPLACE FUNCTION data.calculate_meter_consumption(
    p_meter_id UUID,
    p_period_from DATE,
    p_period_to DATE
)
RETURNS NUMERIC AS $$
DECLARE
    v_consumption NUMERIC;
BEGIN
    SELECT COALESCE(SUM(consumption), 0)
    INTO v_consumption
    FROM data.meter_readings
    WHERE meter_id = p_meter_id
      AND reading_date BETWEEN p_period_from AND p_period_to;
    
    RETURN v_consumption;
END;
$$ LANGUAGE plpgsql;

-- Function: Get meters by building and service
CREATE OR REPLACE FUNCTION data.get_meters_for_reading(
    p_building_id UUID DEFAULT NULL,
    p_service_id UUID DEFAULT NULL
)
RETURNS TABLE (
    meter_id UUID,
    meter_code TEXT,
    unit_id UUID,
    unit_code TEXT,
    service_id UUID,
    service_code TEXT,
    last_reading_date DATE,
    last_index NUMERIC
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        m.id,
        m.meter_code,
        m.unit_id,
        u.code,
        m.service_id,
        s.code,
        (
            SELECT mr.reading_date
            FROM data.meter_readings mr
            WHERE mr.meter_id = m.id
            ORDER BY mr.reading_date DESC
            LIMIT 1
        ),
        (
            SELECT mr.curr_index
            FROM data.meter_readings mr
            WHERE mr.meter_id = m.id
            ORDER BY mr.reading_date DESC
            LIMIT 1
        )
    FROM data.meters m
    JOIN data.units u ON m.unit_id = u.id
    JOIN svc.services s ON m.service_id = s.id
    WHERE m.active = true
      AND m.removed_at IS NULL
      AND (p_building_id IS NULL OR u.building_id = p_building_id)
      AND (p_service_id IS NULL OR m.service_id = p_service_id)
    ORDER BY u.code;
END;
$$ LANGUAGE plpgsql;

