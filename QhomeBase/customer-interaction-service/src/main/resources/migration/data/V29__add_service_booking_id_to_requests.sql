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
