CREATE TABLE IF NOT EXISTS data.account_creation_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resident_id UUID NOT NULL,
    requested_by UUID NOT NULL,
    username VARCHAR(50),
    email VARCHAR(255),
    auto_generate BOOLEAN NOT NULL DEFAULT true,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by UUID,
    rejected_by UUID,
    rejection_reason TEXT,
    approved_at TIMESTAMP WITH TIME ZONE,
    rejected_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_account_creation_requests_resident 
        FOREIGN KEY (resident_id) REFERENCES data.residents(id) ON DELETE CASCADE,
    CONSTRAINT fk_account_creation_requests_requested_by 
        FOREIGN KEY (requested_by) REFERENCES iam.users(id) ON DELETE CASCADE,
    CONSTRAINT fk_account_creation_requests_approved_by 
        FOREIGN KEY (approved_by) REFERENCES iam.users(id) ON DELETE SET NULL,
    CONSTRAINT fk_account_creation_requests_rejected_by 
        FOREIGN KEY (rejected_by) REFERENCES iam.users(id) ON DELETE SET NULL,
    CONSTRAINT chk_account_creation_requests_status 
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX IF NOT EXISTS idx_account_creation_requests_resident_id 
    ON data.account_creation_requests(resident_id);
CREATE INDEX IF NOT EXISTS idx_account_creation_requests_requested_by 
    ON data.account_creation_requests(requested_by);
CREATE INDEX IF NOT EXISTS idx_account_creation_requests_status 
    ON data.account_creation_requests(status);
CREATE INDEX IF NOT EXISTS idx_account_creation_requests_created_at 
    ON data.account_creation_requests(created_at DESC);

