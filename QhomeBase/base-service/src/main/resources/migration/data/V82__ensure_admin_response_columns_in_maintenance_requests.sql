-- V82: Ensure admin response columns exist in maintenance_requests table
-- This migration ensures all admin response related columns are present
-- in case V80 migration did not run successfully

DO $$
BEGIN
    -- Add admin_response column if not exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'data' 
        AND table_name = 'maintenance_requests' 
        AND column_name = 'admin_response'
    ) THEN
        ALTER TABLE data.maintenance_requests
            ADD COLUMN admin_response TEXT;
        RAISE NOTICE 'Added admin_response column';
    END IF;

    -- Add estimated_cost column if not exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'data' 
        AND table_name = 'maintenance_requests' 
        AND column_name = 'estimated_cost'
    ) THEN
        ALTER TABLE data.maintenance_requests
            ADD COLUMN estimated_cost NUMERIC(15, 2);
        RAISE NOTICE 'Added estimated_cost column';
    END IF;

    -- Add responded_by column if not exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'data' 
        AND table_name = 'maintenance_requests' 
        AND column_name = 'responded_by'
    ) THEN
        ALTER TABLE data.maintenance_requests
            ADD COLUMN responded_by UUID;
        RAISE NOTICE 'Added responded_by column';
    END IF;

    -- Add responded_at column if not exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'data' 
        AND table_name = 'maintenance_requests' 
        AND column_name = 'responded_at'
    ) THEN
        ALTER TABLE data.maintenance_requests
            ADD COLUMN responded_at TIMESTAMPTZ;
        RAISE NOTICE 'Added responded_at column';
    END IF;

    -- Add response_status column if not exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'data' 
        AND table_name = 'maintenance_requests' 
        AND column_name = 'response_status'
    ) THEN
        ALTER TABLE data.maintenance_requests
            ADD COLUMN response_status VARCHAR(50);
        RAISE NOTICE 'Added response_status column';
    END IF;
END $$;

-- Add constraint to ensure estimated_cost is non-negative if provided
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'ck_maintenance_estimated_cost'
        AND conrelid = 'data.maintenance_requests'::regclass
    ) THEN
        ALTER TABLE data.maintenance_requests
            ADD CONSTRAINT ck_maintenance_estimated_cost 
            CHECK (estimated_cost IS NULL OR estimated_cost >= 0);
        RAISE NOTICE 'Added estimated_cost constraint';
    END IF;
END $$;

-- Add index for querying requests with pending response approval
CREATE INDEX IF NOT EXISTS idx_maintenance_requests_response_status 
    ON data.maintenance_requests(response_status, created_at) 
    WHERE response_status = 'PENDING_APPROVAL';

-- Add comments if not already present
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
            WHERE attrelid = c.oid AND attname = 'admin_response'
        )
    ) THEN
        COMMENT ON COLUMN data.maintenance_requests.admin_response IS 'Admin/staff response with estimated damage description';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_catalog.pg_description d
        JOIN pg_catalog.pg_class c ON d.objoid = c.oid
        JOIN pg_catalog.pg_namespace n ON c.relnamespace = n.oid
        WHERE n.nspname = 'data'
        AND c.relname = 'maintenance_requests'
        AND d.objsubid = (
            SELECT attnum FROM pg_catalog.pg_attribute 
            WHERE attrelid = c.oid AND attname = 'estimated_cost'
        )
    ) THEN
        COMMENT ON COLUMN data.maintenance_requests.estimated_cost IS 'Estimated cost for the repair';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_catalog.pg_description d
        JOIN pg_catalog.pg_class c ON d.objoid = c.oid
        JOIN pg_catalog.pg_namespace n ON c.relnamespace = n.oid
        WHERE n.nspname = 'data'
        AND c.relname = 'maintenance_requests'
        AND d.objsubid = (
            SELECT attnum FROM pg_catalog.pg_attribute 
            WHERE attrelid = c.oid AND attname = 'responded_by'
        )
    ) THEN
        COMMENT ON COLUMN data.maintenance_requests.responded_by IS 'Admin/staff user ID who responded';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_catalog.pg_description d
        JOIN pg_catalog.pg_class c ON d.objoid = c.oid
        JOIN pg_catalog.pg_namespace n ON c.relnamespace = n.oid
        WHERE n.nspname = 'data'
        AND c.relname = 'maintenance_requests'
        AND d.objsubid = (
            SELECT attnum FROM pg_catalog.pg_attribute 
            WHERE attrelid = c.oid AND attname = 'responded_at'
        )
    ) THEN
        COMMENT ON COLUMN data.maintenance_requests.responded_at IS 'Timestamp when admin/staff responded';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_catalog.pg_description d
        JOIN pg_catalog.pg_class c ON d.objoid = c.oid
        JOIN pg_catalog.pg_namespace n ON c.relnamespace = n.oid
        WHERE n.nspname = 'data'
        AND c.relname = 'maintenance_requests'
        AND d.objsubid = (
            SELECT attnum FROM pg_catalog.pg_attribute 
            WHERE attrelid = c.oid AND attname = 'response_status'
        )
    ) THEN
        COMMENT ON COLUMN data.maintenance_requests.response_status IS 'Status of resident response: PENDING_APPROVAL, APPROVED, REJECTED';
    END IF;
END $$;








































