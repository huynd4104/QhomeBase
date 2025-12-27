-- Add type, fee, and repaired_date columns to requests table
-- All columns are nullable (fee is null when creating a request)
ALTER TABLE cs_service.requests
ADD COLUMN IF NOT EXISTS type VARCHAR(255);

ALTER TABLE cs_service.requests
ADD COLUMN IF NOT EXISTS fee NUMERIC(15, 2);

ALTER TABLE cs_service.requests
ADD COLUMN IF NOT EXISTS repaired_date DATE;

-- Create index on type if needed
CREATE INDEX IF NOT EXISTS idx_requests_type ON cs_service.requests (type);

