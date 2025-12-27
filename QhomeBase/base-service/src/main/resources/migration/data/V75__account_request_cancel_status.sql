ALTER TABLE data.account_creation_requests
    DROP CONSTRAINT IF EXISTS chk_account_creation_requests_status;

ALTER TABLE data.account_creation_requests
    ADD CONSTRAINT chk_account_creation_requests_status
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'));


