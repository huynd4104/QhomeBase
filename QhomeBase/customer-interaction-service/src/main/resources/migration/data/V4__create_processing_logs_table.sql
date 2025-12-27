CREATE TABLE IF NOT EXISTS cs_service.processing_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    record_type cs_service.record_type NOT NULL,
    record_id UUID NOT NULL,
    staff_in_charge UUID,
    content TEXT,
    created_at TIMESTAMP DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_logs_record ON cs_service.processing_logs (record_id, record_type);
