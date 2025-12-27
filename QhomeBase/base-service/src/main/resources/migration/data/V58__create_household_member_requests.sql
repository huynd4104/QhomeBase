-- V56: Create household_member_requests table for resident membership requests
-- Stores requests submitted by primary residents for admins to approve household members

CREATE TABLE IF NOT EXISTS data.household_member_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id UUID NOT NULL,
    resident_id UUID NOT NULL,
    requested_by UUID NOT NULL,
    relation TEXT,
    proof_of_relation_image_url TEXT,
    note TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by UUID,
    rejected_by UUID,
    rejection_reason TEXT,
    approved_at TIMESTAMPTZ,
    rejected_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_hmr_household FOREIGN KEY (household_id) REFERENCES data.households(id) ON DELETE CASCADE,
    CONSTRAINT fk_hmr_resident FOREIGN KEY (resident_id) REFERENCES data.residents(id) ON DELETE CASCADE,
    CONSTRAINT fk_hmr_requested_by FOREIGN KEY (requested_by) REFERENCES iam.users(id) ON DELETE CASCADE,
    CONSTRAINT fk_hmr_approved_by FOREIGN KEY (approved_by) REFERENCES iam.users(id) ON DELETE SET NULL,
    CONSTRAINT fk_hmr_rejected_by FOREIGN KEY (rejected_by) REFERENCES iam.users(id) ON DELETE SET NULL,
    CONSTRAINT ck_hmr_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_hmr_household ON data.household_member_requests (household_id);
CREATE INDEX IF NOT EXISTS idx_hmr_resident ON data.household_member_requests (resident_id);
CREATE INDEX IF NOT EXISTS idx_hmr_requested_by ON data.household_member_requests (requested_by);
CREATE INDEX IF NOT EXISTS idx_hmr_status ON data.household_member_requests (status);
