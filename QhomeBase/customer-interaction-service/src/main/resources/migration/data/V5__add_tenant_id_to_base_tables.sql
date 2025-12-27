ALTER TABLE cs_service.requests
ADD COLUMN tenant_id UUID NOT NULL;
CREATE INDEX IF NOT EXISTS idx_requests_tenant_id ON cs_service.requests (tenant_id);

ALTER TABLE cs_service.complaints
ADD COLUMN tenant_id UUID NOT NULL;
CREATE INDEX IF NOT EXISTS idx_complaints_tenant_id ON cs_service.complaints (tenant_id);