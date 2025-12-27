
DO $do$
BEGIN
    IF EXISTS (
        SELECT 1 
        FROM pg_constraint 
        WHERE conname = 'vehicles_resident_id_fkey' 
          AND conrelid = 'data.vehicles'::regclass
    ) THEN
        ALTER TABLE data.vehicles DROP CONSTRAINT vehicles_resident_id_fkey;
        RAISE NOTICE 'Dropped FK constraint: vehicles_resident_id_fkey';
    ELSE
        RAISE NOTICE 'ℹ FK constraint vehicles_resident_id_fkey does not exist';
    END IF;
END
$do$;












