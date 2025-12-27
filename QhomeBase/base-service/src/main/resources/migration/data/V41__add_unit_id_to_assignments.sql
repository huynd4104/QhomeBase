-- Add unit_ids array to meter_reading_assignments for exclusive handling
-- unit_ids is EXCLUSIVE list (units that should NOT be read in this assignment)
-- Logic: 
--   - unit_ids = NULL AND floor = NULL → all floors (no exclusions)
--   - unit_ids = NULL AND floor = 5 → floor 5 (no exclusions)
--   - unit_ids = [uuid1, uuid2] AND floor = NULL → exclude these units from all floors
--   - unit_ids = [uuid1, uuid2] AND floor = 5 → exclude these units from floor 5

-- Step 1: Add unit_ids array column (nullable)
ALTER TABLE data.meter_reading_assignments
ADD COLUMN IF NOT EXISTS unit_ids UUID[];

-- Step 2: Add check constraint for scope logic
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'ck_assignment_scope'
    ) THEN
        ALTER TABLE data.meter_reading_assignments
        ADD CONSTRAINT ck_assignment_scope
            CHECK (
                (unit_ids IS NULL AND floor IS NULL) OR  -- All floors and units
                (unit_ids IS NULL AND floor IS NOT NULL) OR  -- Specific floor
                (unit_ids IS NOT NULL AND array_length(unit_ids, 1) > 0)  -- At least one unit if array is set
            );
    END IF;
END$$;

-- Step 3: Create GIN index for array queries (efficient for array_contains operations)
CREATE INDEX IF NOT EXISTS idx_assignments_unit_ids 
    ON data.meter_reading_assignments USING GIN(unit_ids) 
    WHERE unit_ids IS NOT NULL;

-- Step 4: Update function to support unit_ids array
DROP FUNCTION IF EXISTS data.get_meters_for_assignment(UUID, UUID, INTEGER);

CREATE OR REPLACE FUNCTION data.get_meters_for_assignment(
    p_building_id UUID,
    p_service_id UUID,
    p_floor INTEGER DEFAULT NULL,
    p_unit_ids UUID[] DEFAULT NULL
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
      AND (
          (p_unit_ids IS NULL AND p_floor IS NOT NULL AND u.floor = p_floor) OR  -- Specific floor (no exclusions)
          (p_unit_ids IS NULL AND p_floor IS NULL) OR  -- All floors (no exclusions)
          (p_unit_ids IS NOT NULL AND u.id != ALL(p_unit_ids))  -- Exclude units in exclusive list
      )
    ORDER BY u.floor, u.code;
END;
$$ LANGUAGE plpgsql STABLE;

COMMENT ON FUNCTION data.get_meters_for_assignment IS 'Get meters for assignment: exclude unit_ids (exclusive) or by floor or all (NULL)';
COMMENT ON COLUMN data.meter_reading_assignments.unit_ids IS 'Array of unit IDs to EXCLUDE (exclusive list, nullable). If set, these units are NOT included in assignment. If NULL, no exclusions.';

-- Step 5: Add trigger to validate foreign key for array elements
CREATE OR REPLACE FUNCTION data.validate_unit_ids_array()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.unit_ids IS NOT NULL THEN
        -- Check if all unit_ids exist in data.units table
        IF EXISTS (
            SELECT 1 
            FROM unnest(NEW.unit_ids) AS unit_id
            WHERE NOT EXISTS (
                SELECT 1 FROM data.units WHERE id = unit_id
            )
        ) THEN
            RAISE EXCEPTION 'One or more unit_ids in array do not exist in data.units table';
        END IF;
        
        -- If building_id is set, validate all units belong to that building
        IF NEW.building_id IS NOT NULL THEN
            IF EXISTS (
                SELECT 1 
                FROM unnest(NEW.unit_ids) AS unit_id
                WHERE NOT EXISTS (
                    SELECT 1 FROM data.units 
                    WHERE id = unit_id AND building_id = NEW.building_id
                )
            ) THEN
                RAISE EXCEPTION 'One or more unit_ids do not belong to the specified building_id';
            END IF;
        END IF;
        
        -- If floor is set, validate all units are on that floor
        IF NEW.floor IS NOT NULL THEN
            IF EXISTS (
                SELECT 1 
                FROM unnest(NEW.unit_ids) AS unit_id
                WHERE NOT EXISTS (
                    SELECT 1 FROM data.units 
                    WHERE id = unit_id AND floor = NEW.floor
                )
            ) THEN
                RAISE EXCEPTION 'One or more unit_ids are not on the specified floor';
            END IF;
        END IF;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_validate_unit_ids_array ON data.meter_reading_assignments;

CREATE TRIGGER trg_validate_unit_ids_array
    BEFORE INSERT OR UPDATE ON data.meter_reading_assignments
    FOR EACH ROW
    EXECUTE FUNCTION data.validate_unit_ids_array();

COMMENT ON FUNCTION data.validate_unit_ids_array IS 'Validate that all unit_ids in array exist and belong to correct building/floor';

