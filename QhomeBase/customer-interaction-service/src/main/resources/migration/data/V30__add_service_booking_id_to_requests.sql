-- Add service_booking_id column to requests table
-- This column links feedback/complaint requests to service bookings
ALTER TABLE cs_service.requests
ADD COLUMN IF NOT EXISTS service_booking_id UUID;

-- Create index to optimize queries by service booking
CREATE INDEX IF NOT EXISTS idx_requests_service_booking_id 
ON cs_service.requests(service_booking_id);

-- Add comment
COMMENT ON COLUMN cs_service.requests.service_booking_id IS 
'ID của service booking mà phản ánh này liên quan đến (nullable). Dùng để liên kết phản ánh với dịch vụ đã sử dụng trong tiện ích nội khu.';

-- Ensure fee column exists (in case V11 didn't run)
ALTER TABLE cs_service.requests
ADD COLUMN IF NOT EXISTS fee NUMERIC(15, 2);

-- Ensure type column exists (in case V11 didn't run)
ALTER TABLE cs_service.requests
ADD COLUMN IF NOT EXISTS type VARCHAR(255);

-- Ensure repaired_date column exists (in case V11 didn't run)
ALTER TABLE cs_service.requests
ADD COLUMN IF NOT EXISTS repaired_date DATE;

-- Create index on type if needed
CREATE INDEX IF NOT EXISTS idx_requests_type ON cs_service.requests (type);
