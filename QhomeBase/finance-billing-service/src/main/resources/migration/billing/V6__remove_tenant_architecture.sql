-- V6: Remove tenant architecture from billing schema
-- Remove tenant_id from all billing schema tables

-- Drop tenant_id columns if they exist
ALTER TABLE IF EXISTS billing.billing_cycles DROP COLUMN IF EXISTS tenant_id CASCADE;
ALTER TABLE IF EXISTS billing.cycle_services DROP COLUMN IF EXISTS tenant_id CASCADE;

-- Drop any tenant-related indexes
DROP INDEX IF EXISTS billing.idx_billing_cycles_tenant;
DROP INDEX IF EXISTS billing.idx_cycle_services_tenant;

