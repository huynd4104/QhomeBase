ALTER TABLE files.contracts
    ADD COLUMN IF NOT EXISTS renewal_reminder_sent_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS renewal_declined_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS renewal_status VARCHAR(20) DEFAULT 'PENDING';

ALTER TABLE files.contracts
    ADD CONSTRAINT ck_contracts_renewal_status 
    CHECK (renewal_status IS NULL OR renewal_status IN ('PENDING', 'REMINDED', 'DECLINED', 'EXTENDED'));

CREATE INDEX IF NOT EXISTS idx_contracts_renewal_status ON files.contracts(renewal_status) 
    WHERE renewal_status IN ('PENDING', 'REMINDED');

CREATE INDEX IF NOT EXISTS idx_contracts_end_date_renewal ON files.contracts(end_date, renewal_status) 
    WHERE end_date IS NOT NULL AND status = 'ACTIVE';

