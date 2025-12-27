ALTER TABLE billing.billing_cycles
    ADD COLUMN IF NOT EXISTS external_cycle_id UUID;

CREATE UNIQUE INDEX IF NOT EXISTS idx_billing_cycles_external_cycle
    ON billing.billing_cycles (external_cycle_id)
    WHERE external_cycle_id IS NOT NULL;

