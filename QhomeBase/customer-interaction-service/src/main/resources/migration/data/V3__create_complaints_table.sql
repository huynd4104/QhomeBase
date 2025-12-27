CREATE TABLE IF NOT EXISTS cs_service.complaints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resident_id UUID NOT NULL,
    resident_name VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    image_path TEXT,
    content TEXT NOT NULL,
    status cs_service.complaint_status NOT NULL,
    priority cs_service.priority_level NOT NULL,
    final_resolution_plan TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_comp_status ON cs_service.complaints (status);
