-- V75: Add service_id to reading_cycles and split cycles by service

-- Step 1: Add service_id column (nullable first for migration)
ALTER TABLE data.reading_cycles 
ADD COLUMN IF NOT EXISTS service_id UUID;

-- Step 2: Drop old unique constraint
ALTER TABLE data.reading_cycles 
DROP CONSTRAINT IF EXISTS uq_reading_cycle_name;

-- Step 3: Add foreign key constraint (drop if exists first)
ALTER TABLE data.reading_cycles
DROP CONSTRAINT IF EXISTS fk_reading_cycle_service;

ALTER TABLE data.reading_cycles
ADD CONSTRAINT fk_reading_cycle_service 
FOREIGN KEY (service_id) REFERENCES data.services(id);

-- Step 4: Migrate existing cycles
-- For each existing cycle, create separate cycles for each service that requires meter reading
-- This script assumes we have WATER and ELECTRIC services (adjust service codes as needed)
DO $$
DECLARE
    cycle_record RECORD;
    service_record RECORD;
    new_cycle_id UUID;
    new_cycle_name TEXT;
BEGIN
    -- Loop through all existing cycles
    FOR cycle_record IN SELECT * FROM data.reading_cycles WHERE service_id IS NULL
    LOOP
        -- Loop through all services that require meter reading
        FOR service_record IN 
            SELECT id, code, name 
            FROM data.services 
            WHERE requires_meter = true AND active = true
        LOOP
            -- Create new cycle name with service suffix for uniqueness
            new_cycle_name := cycle_record.name || '-' || service_record.code;
            
            -- Check if cycle with this name and service already exists
            IF NOT EXISTS (
                SELECT 1 FROM data.reading_cycles 
                WHERE name = new_cycle_name AND service_id = service_record.id
            ) THEN
                -- Create new cycle for this service
                INSERT INTO data.reading_cycles (
                    id,
                    name,
                    period_from,
                    period_to,
                    status,
                    description,
                    created_by,
                    created_at,
                    updated_at,
                    service_id
                ) VALUES (
                    gen_random_uuid(),
                    new_cycle_name,
                    cycle_record.period_from,
                    cycle_record.period_to,
                    cycle_record.status,
                    COALESCE(cycle_record.description, '') || ' (Service: ' || service_record.name || ')',
                    cycle_record.created_by,
                    cycle_record.created_at,
                    cycle_record.updated_at,
                    service_record.id
                ) RETURNING id INTO new_cycle_id;
                
                -- Update assignments to point to the new cycle for matching service
                UPDATE data.meter_reading_assignments
                SET cycle_id = new_cycle_id
                WHERE cycle_id = cycle_record.id
                  AND service_id = service_record.id;

                -- Update meter readings so they reference the new cycle as well
                UPDATE data.meter_readings mr
                SET cycle_id = new_cycle_id
                FROM data.meter_reading_assignments mra
                WHERE mr.assignment_id = mra.id
                  AND mra.cycle_id = new_cycle_id
                  AND mr.cycle_id = cycle_record.id;
                  
                RAISE NOTICE 'Created cycle % for service % (original cycle: %)', 
                    new_cycle_name, service_record.code, cycle_record.name;
            END IF;
        END LOOP;
        
        -- Delete old cycle if no assignments or readings remain
        IF NOT EXISTS (
            SELECT 1 FROM data.meter_reading_assignments 
            WHERE cycle_id = cycle_record.id
        ) AND NOT EXISTS (
            SELECT 1 FROM data.meter_readings
            WHERE cycle_id = cycle_record.id
        ) THEN
            DELETE FROM data.reading_cycles WHERE id = cycle_record.id;
            RAISE NOTICE 'Deleted old cycle % (no assignments or readings remaining)', cycle_record.name;
        ELSE
            -- Keep old cycle but mark it (in case some assignments don't have service match)
            RAISE WARNING 'Old cycle % still has assignments without service match', cycle_record.name;
        END IF;
    END LOOP;
END $$;

-- Step 5: Make service_id NOT NULL (after migration)
ALTER TABLE data.reading_cycles
ALTER COLUMN service_id SET NOT NULL;

-- Step 6: Create new unique constraint (name + service_id)
ALTER TABLE data.reading_cycles
DROP CONSTRAINT IF EXISTS uq_reading_cycle_name_service;

ALTER TABLE data.reading_cycles
ADD CONSTRAINT uq_reading_cycle_name_service UNIQUE (name, service_id);

-- Step 7: Create index for better query performance
CREATE INDEX IF NOT EXISTS idx_reading_cycles_service_id 
ON data.reading_cycles(service_id);

CREATE INDEX IF NOT EXISTS idx_reading_cycles_status_service 
ON data.reading_cycles(status, service_id);


