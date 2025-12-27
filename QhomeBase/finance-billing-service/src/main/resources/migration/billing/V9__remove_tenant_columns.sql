-- Remove tenant_id from billing schema tables no longer using tenant scoping

ALTER TABLE IF EXISTS billing.invoices
    DROP COLUMN IF EXISTS tenant_id CASCADE;

ALTER TABLE IF EXISTS billing.invoice_lines
    DROP COLUMN IF EXISTS tenant_id CASCADE;

ALTER TABLE IF EXISTS billing.billing_cycles
    DROP COLUMN IF EXISTS tenant_id CASCADE;

-- Optional: pricing is now global; drop tenant_id if present
ALTER TABLE IF EXISTS billing.service_pricing
    DROP COLUMN IF EXISTS tenant_id CASCADE;


