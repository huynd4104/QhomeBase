ALTER TABLE data.maintenance_requests
    ADD COLUMN IF NOT EXISTS preferred_datetime TIMESTAMPTZ;


