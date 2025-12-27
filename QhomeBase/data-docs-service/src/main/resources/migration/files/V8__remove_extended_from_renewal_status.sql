UPDATE files.contracts
SET renewal_status = 'PENDING'
WHERE renewal_status = 'EXTENDED';

ALTER TABLE files.contracts
    DROP CONSTRAINT IF EXISTS ck_contracts_renewal_status;

ALTER TABLE files.contracts
    ADD CONSTRAINT ck_contracts_renewal_status 
    CHECK (renewal_status IS NULL OR renewal_status IN ('PENDING', 'REMINDED', 'DECLINED'));

