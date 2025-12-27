-- Simplify common_area_maintenance_requests table
-- Remove columns that are no longer used after simplifying the flow
-- The simplified flow only requires: admin view, approve (change status), deny (change status), complete (change status)
-- No need for: estimated_cost, responded_by, responded_at, response_status, progress_notes, assigned_to, completed_at

-- Drop index on assigned_to (will be removed)
DROP INDEX IF EXISTS data.idx_common_area_maintenance_requests_assigned;

-- Remove columns that are no longer used
-- Note: Comments on these columns will be automatically removed when columns are dropped
ALTER TABLE data.common_area_maintenance_requests
    DROP COLUMN IF EXISTS estimated_cost,
    DROP COLUMN IF EXISTS responded_by,
    DROP COLUMN IF EXISTS responded_at,
    DROP COLUMN IF EXISTS response_status,
    DROP COLUMN IF EXISTS progress_notes,
    DROP COLUMN IF EXISTS assigned_to,
    DROP COLUMN IF EXISTS completed_at;
