ALTER TABLE data.households
    ADD COLUMN IF NOT EXISTS contract_id UUID;

CREATE INDEX IF NOT EXISTS idx_households_contract_id
    ON data.households (contract_id);




