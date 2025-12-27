ALTER TABLE data.cleaning_requests
ADD COLUMN user_id UUID;

ALTER TABLE data.maintenance_requests
ADD COLUMN user_id UUID;

