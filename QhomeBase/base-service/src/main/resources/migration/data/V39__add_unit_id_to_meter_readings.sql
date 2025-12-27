-- Add unit_id to meter_readings for performance optimization
-- This allows direct filtering by unit without joining through meters table

-- Step 1: Add unit_id column (nullable initially for data migration)
ALTER TABLE data.meter_readings
ADD COLUMN IF NOT EXISTS unit_id UUID;

-- Step 2: Populate unit_id from meters table for existing readings
UPDATE data.meter_readings mr
SET unit_id = m.unit_id
FROM data.meters m
WHERE mr.meter_id = m.id
  AND mr.unit_id IS NULL;

-- Step 3: Verify all readings have unit_id before setting NOT NULL
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM data.meter_readings 
        WHERE unit_id IS NULL
    ) THEN
        RAISE EXCEPTION 'Cannot set unit_id NOT NULL: found readings with NULL unit_id';
    END IF;
END$$;

-- Step 4: Make unit_id NOT NULL after data migration
ALTER TABLE data.meter_readings
ALTER COLUMN unit_id SET NOT NULL;

-- Step 5: Add foreign key constraint
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_reading_unit'
    ) THEN
        ALTER TABLE data.meter_readings
        ADD CONSTRAINT fk_reading_unit
            FOREIGN KEY (unit_id) REFERENCES data.units(id) ON DELETE CASCADE;
    END IF;
END$$;

-- Step 6: Create index for performance (unit_id, reading_date)
CREATE INDEX IF NOT EXISTS idx_readings_unit_date
    ON data.meter_readings(unit_id, reading_date DESC);

-- Step 7: Create composite index for common queries (unit_id, verified, reading_date)
CREATE INDEX IF NOT EXISTS idx_readings_unit_verified_date
    ON data.meter_readings(unit_id, verified, reading_date DESC)
    WHERE verified = false;

-- Step 8: Update v_meter_readings_detail view to use direct unit_id
DROP VIEW IF EXISTS data.v_meter_readings_detail CASCADE;

CREATE OR REPLACE VIEW data.v_meter_readings_detail AS
SELECT 
    mr.id,
    mr.meter_id,
    m.meter_code,
    mr.unit_id,
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
JOIN data.units u ON mr.unit_id = u.id
JOIN data.buildings b ON u.building_id = b.id
JOIN svc.services s ON m.service_id = s.id;

COMMENT ON VIEW data.v_meter_readings_detail IS 'Meter readings with details (using direct unit_id join)';
COMMENT ON COLUMN data.meter_readings.unit_id IS 'Direct reference to unit for performance (denormalized from meter.unit_id)';

 