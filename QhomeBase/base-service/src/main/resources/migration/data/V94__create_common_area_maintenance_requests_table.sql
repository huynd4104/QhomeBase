-- Create table for common area maintenance requests
-- This table stores maintenance requests for common areas (not unit-specific)
-- Examples: hallways, elevators, common lighting, parking areas, common entrances, landscaping, etc.

CREATE TABLE IF NOT EXISTS data.common_area_maintenance_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    building_id UUID REFERENCES data.buildings(id),
    resident_id UUID,
    created_by UUID NOT NULL,
    area_type TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    attachments TEXT, -- JSON array of attachment URLs
    location TEXT NOT NULL,
    contact_name TEXT NOT NULL,
    contact_phone TEXT NOT NULL,
    user_id UUID,
    note TEXT,
    status TEXT NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    admin_response TEXT,
    estimated_cost NUMERIC(15, 2),
    responded_by UUID,
    responded_at TIMESTAMPTZ,
    response_status TEXT,
    progress_notes TEXT,
    assigned_to UUID,
    completed_at TIMESTAMPTZ
);

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_common_area_maintenance_requests_resident 
    ON data.common_area_maintenance_requests(resident_id);
    
CREATE INDEX IF NOT EXISTS idx_common_area_maintenance_requests_status 
    ON data.common_area_maintenance_requests(status);
    
CREATE INDEX IF NOT EXISTS idx_common_area_maintenance_requests_building 
    ON data.common_area_maintenance_requests(building_id);
    
CREATE INDEX IF NOT EXISTS idx_common_area_maintenance_requests_assigned 
    ON data.common_area_maintenance_requests(assigned_to);
    
CREATE INDEX IF NOT EXISTS idx_common_area_maintenance_requests_created_at 
    ON data.common_area_maintenance_requests(created_at DESC);

-- Add comments for documentation
COMMENT ON TABLE data.common_area_maintenance_requests IS 
    'Stores maintenance requests for common areas (not unit-specific) such as hallways, elevators, common lighting, parking areas, etc.';
    
COMMENT ON COLUMN data.common_area_maintenance_requests.area_type IS 
    'Type of common area: Hành lang, Thang máy, Đèn khu vực chung, Bãi xe, Cửa ra vào chung, Cảnh quan, etc.';
    
COMMENT ON COLUMN data.common_area_maintenance_requests.attachments IS 
    'JSON array of attachment URLs (images/videos)';
    
COMMENT ON COLUMN data.common_area_maintenance_requests.status IS 
    'Request status: PENDING, IN_PROGRESS, COMPLETED, CANCELLED, REJECTED';
    
COMMENT ON COLUMN data.common_area_maintenance_requests.response_status IS 
    'Response status: PENDING_APPROVAL, APPROVED, REJECTED';
