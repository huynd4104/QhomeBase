-- Drop dependent views, remove session.status, recreate views using completed_at

-- 1) Drop dependent views first (if exist)
DROP VIEW IF EXISTS data.v_reading_sessions_progress CASCADE;
DROP VIEW IF EXISTS data.v_reading_cycles_progress CASCADE;

-- 2) Remove old CHECK and status column (idempotent)
ALTER TABLE data.meter_reading_sessions
    DROP CONSTRAINT IF EXISTS ck_session_status;

ALTER TABLE data.meter_reading_sessions
    DROP COLUMN IF EXISTS status;

-- 3) Recreate views without depending on the status column

-- Session-level progress (no assignment_id reference)
CREATE OR REPLACE VIEW data.v_reading_sessions_progress AS
SELECT
    s.id                                          AS session_id,
    s.cycle_id,
    s.building_id,
    s.service_id,
    s.reader_id,
    s.started_at,
    s.completed_at,
    CASE WHEN s.completed_at IS NULL THEN 'IN_PROGRESS' ELSE 'COMPLETED' END AS status,
    s.units_read
FROM data.meter_reading_sessions s;

-- Cycle-level progress aggregated from sessions
CREATE OR REPLACE VIEW data.v_reading_cycles_progress AS
SELECT
    c.id                                                AS cycle_id,
    COUNT(s.id)                                         AS total_sessions,
    COUNT(*) FILTER (WHERE s.completed_at IS NULL)      AS active_sessions,
    COUNT(*) FILTER (WHERE s.completed_at IS NOT NULL)  AS completed_sessions
FROM data.reading_cycles c
LEFT JOIN data.meter_reading_sessions s ON s.cycle_id = c.id
GROUP BY c.id;


