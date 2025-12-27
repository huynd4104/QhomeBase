-- V6: Remove tenant architecture from customer-interaction-service
-- Remove tenant_id from cs_service schema tables

-- Drop tenant_id columns
ALTER TABLE IF EXISTS cs_service.requests DROP COLUMN IF EXISTS tenant_id CASCADE;
ALTER TABLE IF EXISTS cs_service.complaints DROP COLUMN IF EXISTS tenant_id CASCADE;
ALTER TABLE IF EXISTS cs_service.processing_logs DROP COLUMN IF EXISTS tenant_id CASCADE;

-- Drop any tenant-related indexes
DROP INDEX IF EXISTS cs_service.idx_requests_tenant_status;
DROP INDEX IF EXISTS cs_service.idx_complaints_tenant_status;

