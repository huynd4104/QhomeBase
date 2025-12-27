ALTER TABLE data.account_creation_requests
    ADD COLUMN IF NOT EXISTS password TEXT;

