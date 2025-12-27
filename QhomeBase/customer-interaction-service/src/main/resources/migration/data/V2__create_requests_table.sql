CREATE TABLE IF NOT EXISTS cs_service.requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resident_id UUID NOT NULL,
    resident_name VARCHAR(255) NOT NULL,
    image_path TEXT,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    status cs_service.request_status NOT NULL,
    priority cs_service.priority_level NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_req_status ON cs_service.requests (status);