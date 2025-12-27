-- Remove meter_reading_sessions table and session_id from meter_readings
-- Sessions are redundant - readings can link directly to assignments

-- Step 1: Drop views that depend on sessions
DROP VIEW IF EXISTS data.v_reading_sessions_progress CASCADE;
DROP VIEW IF EXISTS data.v_reading_cycles_progress CASCADE;
DROP VIEW IF EXISTS data.v_meter_readings_detail CASCADE;

-- Step 2: Drop foreign key constraint from meter_readings.session_id
ALTER TABLE data.meter_readings
DROP CONSTRAINT IF EXISTS fk_meter_readings_session;

-- Step 3: Drop index on session_id
DROP INDEX IF EXISTS data.idx_readings_session;

-- Step 4: Drop session_id column from meter_readings
ALTER TABLE data.meter_readings
DROP COLUMN IF EXISTS session_id;

-- Step 5: Drop meter_reading_sessions table
DROP TABLE IF EXISTS data.meter_reading_sessions CASCADE;

-- Step 6: Recreate v_reading_cycles_progress view without sessions
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
    COUNT(DISTINCT mr.id) AS readings_count,
    COUNT(DISTINCT CASE WHEN mr.verified THEN mr.id END) AS readings_verified,
    rc.created_by,
    rc.created_at
FROM data.reading_cycles rc
LEFT JOIN data.meter_reading_assignments mra ON mra.cycle_id = rc.id
LEFT JOIN data.meter_readings mr ON mr.assignment_id = mra.id
GROUP BY rc.id, rc.name, rc.period_from, rc.period_to, rc.status, 
         rc.description, rc.created_by, rc.created_at;

COMMENT ON VIEW data.v_reading_cycles_progress IS 'Reading cycles with progress (without sessions)';

-- Step 7: Recreate v_meter_readings_detail view without session_id
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
    mr.assignment_id
FROM data.meter_readings mr
JOIN data.meters m ON mr.meter_id = m.id
JOIN data.units u ON m.unit_id = u.id
JOIN data.buildings b ON u.building_id = b.id
JOIN svc.services s ON m.service_id = s.id;

COMMENT ON VIEW data.v_meter_readings_detail IS 'Meter readings with details (without sessions)';

