-- V80: Add admin response fields to maintenance_requests table
-- These fields allow admin/staff to respond with estimated damage and cost,
-- and allow residents to approve or reject the response

ALTER TABLE data.maintenance_requests
    ADD COLUMN IF NOT EXISTS admin_response TEXT,
    ADD COLUMN IF NOT EXISTS estimated_cost NUMERIC(15, 2),
    ADD COLUMN IF NOT EXISTS responded_by UUID,
    ADD COLUMN IF NOT EXISTS responded_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS response_status VARCHAR(50);

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
    END IF;
END $$;

-- Add index for querying requests with pending response approval
CREATE INDEX IF NOT EXISTS idx_maintenance_requests_response_status 
    ON data.maintenance_requests(response_status, created_at) 
    WHERE response_status = 'PENDING_APPROVAL';

COMMENT ON COLUMN data.maintenance_requests.admin_response IS 'Admin/staff response with estimated damage description';
COMMENT ON COLUMN data.maintenance_requests.estimated_cost IS 'Estimated cost for the repair';
COMMENT ON COLUMN data.maintenance_requests.responded_by IS 'Admin/staff user ID who responded';
COMMENT ON COLUMN data.maintenance_requests.responded_at IS 'Timestamp when admin/staff responded';
COMMENT ON COLUMN data.maintenance_requests.response_status IS 'Status of resident response: PENDING_APPROVAL, APPROVED, REJECTED';

