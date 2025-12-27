-- Remove complex views for meter reading status
-- Keep only simple query logic in service layer
-- Simple logic: LEFT JOIN meter_readings - if exists = READ, if not = PENDING

-- Step 1: Drop all complex views (created in V42, V43, V44)
DROP VIEW IF EXISTS data.v_meters_with_reading_status CASCADE;
DROP VIEW IF EXISTS data.v_meter_reading_status CASCADE;
DROP VIEW IF EXISTS data.v_meter_reading_status_by_cycle CASCADE;
DROP VIEW IF EXISTS data.v_meter_latest_reading_status CASCADE;

-- Step 2: Drop all complex functions
DROP FUNCTION IF EXISTS data.get_meters_with_status_for_assignment(UUID);
DROP FUNCTION IF EXISTS data.get_meter_reading_status_by_assignment(UUID);

-- Step 3: Keep simple function for getting meters (without status)
-- This function is useful and simple, keep it
-- get_meters_for_assignment() - already exists in V41

COMMENT ON FUNCTION data.get_meters_for_assignment IS 'Get meters for assignment (simple query). Use LEFT JOIN meter_readings in service layer to determine status (READ/PENDING).';

-- Step 4: Note for developers
-- Status checking is done in service layer with simple query:
-- 
-- SELECT 
--     m.id AS meter_id,
--     m.meter_code,
--     m.unit_id,
--     u.code AS unit_code,
--     u.floor,
--     m.service_id,
--     s.code AS service_code,
--     s.name AS service_name,
--     CASE 
--         WHEN mr.id IS NOT NULL THEN 'READ'
--         ELSE 'PENDING'
--     END AS reading_status,
--     mr.id AS reading_id,
--     mr.reading_date,
--     mr.curr_index,
--     mr.verified,
--     mr.disputed
-- FROM data.meters m
-- INNER JOIN data.units u ON m.unit_id = u.id
-- INNER JOIN svc.services s ON m.service_id = s.id
-- LEFT JOIN data.meter_readings mr ON (
--     mr.meter_id = m.id 
--     AND mr.assignment_id = :assignment_id
-- )
-- WHERE m.active = TRUE
--   AND (
--     -- Filter by assignment scope (floor, unit_ids, or all)
--     -- Logic handled in service layer
--   )
-- ORDER BY u.floor, u.code, m.meter_code;

