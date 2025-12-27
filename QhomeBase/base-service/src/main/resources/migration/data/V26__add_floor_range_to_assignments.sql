ALTER TABLE data.meter_reading_assignments
ADD COLUMN floor_from INTEGER,
ADD COLUMN floor_to INTEGER;

ALTER TABLE data.meter_reading_assignments
ADD CONSTRAINT ck_floor_range 
    CHECK (
        (floor_from IS NULL AND floor_to IS NULL) 
        OR (floor_from IS NOT NULL AND floor_to IS NOT NULL AND floor_from <= floor_to)
    );

CREATE INDEX idx_assignments_building_service_floor 
    ON data.meter_reading_assignments(building_id, service_id, floor_from, floor_to)
    WHERE floor_from IS NOT NULL;

COMMENT ON COLUMN data.meter_reading_assignments.floor_from IS 'Starting floor number for assignment scope (nullable = all floors)';
COMMENT ON COLUMN data.meter_reading_assignments.floor_to IS 'Ending floor number for assignment scope (nullable = all floors)';

CREATE OR REPLACE FUNCTION data.get_meters_for_assignment(
    p_building_id UUID,
    p_service_id UUID,
    p_floor_from INTEGER DEFAULT NULL,
    p_floor_to INTEGER DEFAULT NULL
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
      AND (p_floor_from IS NULL OR u.floor >= p_floor_from)
      AND (p_floor_to IS NULL OR u.floor <= p_floor_to)
    ORDER BY u.floor, u.code;
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION data.get_meters_for_assignment IS 'Get all active meters for a building/service within optional floor range';

CREATE OR REPLACE FUNCTION data.validate_assignment_overlap(
    p_cycle_id UUID,
    p_building_id UUID,
    p_service_id UUID,
    p_floor_from INTEGER,
    p_floor_to INTEGER,
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
      AND (
          (floor_from IS NULL AND floor_to IS NULL)
          OR (p_floor_from IS NULL AND p_floor_to IS NULL)
          OR (
              (floor_from <= p_floor_to OR p_floor_to IS NULL)
              AND (floor_to >= p_floor_from OR p_floor_from IS NULL)
          )
      );
    
    RETURN v_overlap_count = 0;
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION data.validate_assignment_overlap IS 'Check if assignment floor range overlaps with existing assignments in the same cycle/building/service';

