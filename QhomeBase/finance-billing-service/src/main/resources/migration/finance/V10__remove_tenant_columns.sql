-- Remove tenant_id from finance schema tables (renumbered to avoid conflicts)

ALTER TABLE IF EXISTS finance.payments
    DROP COLUMN IF EXISTS tenant_id CASCADE;

ALTER TABLE IF EXISTS finance.payment_allocations
    DROP COLUMN IF EXISTS tenant_id CASCADE;


