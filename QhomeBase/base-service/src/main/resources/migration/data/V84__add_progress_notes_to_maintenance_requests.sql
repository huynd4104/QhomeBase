-- V84: Add progress_notes column to maintenance_requests table
-- This column stores notes from staff/admin during IN_PROGRESS status

DO $$
BEGIN
    -- Add progress_notes column if not exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'data' 
        AND table_name = 'maintenance_requests' 
        AND column_name = 'progress_notes'
    ) THEN
        ALTER TABLE data.maintenance_requests
            ADD COLUMN progress_notes TEXT;
        RAISE NOTICE 'Added progress_notes column';
    END IF;
END $$;

-- Add comment
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_catalog.pg_description d
        JOIN pg_catalog.pg_class c ON d.objoid = c.oid
        JOIN pg_catalog.pg_namespace n ON c.relnamespace = n.oid
        WHERE n.nspname = 'data'
        AND c.relname = 'maintenance_requests'
        AND d.objsubid = (
            SELECT attnum FROM pg_catalog.pg_attribute 
            WHERE attrelid = c.oid AND attname = 'progress_notes'
        )
    ) THEN
        COMMENT ON COLUMN data.maintenance_requests.progress_notes IS 'Notes from staff/admin during repair progress (when status = IN_PROGRESS)';
    END IF;
END $$;

