-- V55: Add cycle_id to meter_readings and ensure synchronization with assignments
-- This migration adds the cycle_id column, backfills existing data, and keeps it in sync
-- with meter_reading_assignments through a trigger.

-- 1. Add column
ALTER TABLE data.meter_readings
    ADD COLUMN IF NOT EXISTS cycle_id UUID;

-- 2. Backfill existing data from assignments
UPDATE data.meter_readings mr
SET cycle_id = mra.cycle_id
FROM data.meter_reading_assignments mra
WHERE mr.assignment_id = mra.id
  AND mr.cycle_id IS NULL;

-- 3. Add foreign key (restrict deletion of cycles that have readings)
ALTER TABLE data.meter_readings
    DROP CONSTRAINT IF EXISTS fk_meter_readings_cycle;

ALTER TABLE data.meter_readings
    ADD CONSTRAINT fk_meter_readings_cycle
        FOREIGN KEY (cycle_id) REFERENCES data.reading_cycles(id) ON DELETE RESTRICT;

-- 4. Update unique constraint to include cycle_id
ALTER TABLE data.meter_readings
    DROP CONSTRAINT IF EXISTS uq_meter_reading;

ALTER TABLE data.meter_readings
    ADD CONSTRAINT uq_meter_reading_meter_date_cycle
        UNIQUE (meter_id, reading_date, cycle_id);

-- 5. Create index for faster lookups by cycle
CREATE INDEX IF NOT EXISTS idx_meter_readings_cycle
    ON data.meter_readings(cycle_id);

-- 6. Trigger to keep cycle_id in sync with assignment
CREATE OR REPLACE FUNCTION data.sync_meter_readings_cycle()
RETURNS TRIGGER AS
$$
BEGIN
    IF NEW.assignment_id IS NOT NULL THEN
        SELECT cycle_id INTO NEW.cycle_id
        FROM data.meter_reading_assignments
        WHERE id = NEW.assignment_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_sync_meter_readings_cycle ON data.meter_readings;

CREATE TRIGGER trg_sync_meter_readings_cycle
    BEFORE INSERT OR UPDATE OF assignment_id ON data.meter_readings
    FOR EACH ROW
    EXECUTE FUNCTION data.sync_meter_readings_cycle();

-- 7. Document the column
COMMENT ON COLUMN data.meter_readings.cycle_id IS 'Reading cycle ID (sync with assignment when present)';





