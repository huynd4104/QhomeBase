ALTER TABLE IF EXISTS billing.billing_cycles DROP CONSTRAINT IF EXISTS uq_billing_cycles;

ALTER TABLE IF EXISTS billing.billing_cycles 
ADD CONSTRAINT uq_billing_cycles UNIQUE (name, period_from, period_to);

