-- V5: Remove tenant architecture from finance schema
-- Remove tenant_id from all finance schema tables

-- Drop tenant_id columns if they exist
ALTER TABLE IF EXISTS finance.service_pricings DROP COLUMN IF EXISTS tenant_id CASCADE;
ALTER TABLE IF EXISTS finance.invoices DROP COLUMN IF EXISTS tenant_id CASCADE;
ALTER TABLE IF EXISTS finance.invoice_lines DROP COLUMN IF EXISTS tenant_id CASCADE;
ALTER TABLE IF EXISTS finance.payments DROP COLUMN IF EXISTS tenant_id CASCADE;
ALTER TABLE IF EXISTS finance.payment_allocations DROP COLUMN IF EXISTS tenant_id CASCADE;

-- Drop any tenant-related indexes
DROP INDEX IF EXISTS finance.idx_invoices_tenant_status;
DROP INDEX IF EXISTS finance.idx_payments_tenant_status;
DROP INDEX IF EXISTS finance.idx_service_pricings_tenant;

