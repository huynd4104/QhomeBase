-- Migration V8 - Fix tenant_deletion_status column type
-- This migration changes the status column from ENUM to VARCHAR

ALTER TABLE data.tenant_deletion_requests 
ALTER COLUMN status TYPE VARCHAR(20) USING status::VARCHAR(20);