ALTER TABLE data.maintenance_requests
    ADD COLUMN IF NOT EXISTS title TEXT,
    ADD COLUMN IF NOT EXISTS status TEXT;


