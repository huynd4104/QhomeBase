-- Function to get meters for an assignment (already exists in V41)
-- This migration adds a note and ensures the function is available

-- Function get_meters_for_assignment() already exists in V41
-- It takes: building_id, service_id, floor, unit_ids[]
-- Returns: meters in scope of assignment

-- Example usage:
-- SELECT * FROM data.get_meters_for_assignment(
--     p_building_id := 'building-uuid',
--     p_service_id := 'service-uuid',
--     p_floor := 5,  -- or NULL for all floors
--     p_unit_ids := ARRAY['unit-uuid-1', 'unit-uuid-2']::UUID[]  -- or NULL for all units
-- );

COMMENT ON FUNCTION data.get_meters_for_assignment IS 
'Get meters in scope of an assignment. 
Logic:
- If unit_ids IS NOT NULL: return meters for those specific units (exception)
- Else if floor IS NOT NULL: return meters for that floor
- Else: return all meters in building/service
Always filters by: service_id, building_id, active = true';


