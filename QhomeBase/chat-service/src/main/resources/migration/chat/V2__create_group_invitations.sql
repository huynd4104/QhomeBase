-- Group invitations table
CREATE TABLE IF NOT EXISTS chat_service.group_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL REFERENCES chat_service.groups(id) ON DELETE CASCADE,
    inviter_id UUID NOT NULL,
    invitee_phone VARCHAR(20) NOT NULL,
    invitee_resident_id UUID, -- Will be set when resident accepts
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, ACCEPTED, DECLINED, EXPIRED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE DEFAULT (CURRENT_TIMESTAMP + INTERVAL '7 days'),
    responded_at TIMESTAMP WITH TIME ZONE
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_group_invitations_group_id ON chat_service.group_invitations(group_id);
CREATE INDEX IF NOT EXISTS idx_group_invitations_invitee_phone ON chat_service.group_invitations(invitee_phone);
CREATE INDEX IF NOT EXISTS idx_group_invitations_invitee_resident_id ON chat_service.group_invitations(invitee_resident_id);
CREATE INDEX IF NOT EXISTS idx_group_invitations_status ON chat_service.group_invitations(status);

-- Unique constraint for pending invitations only (partial unique index)
CREATE UNIQUE INDEX IF NOT EXISTS idx_group_invitations_unique_pending 
    ON chat_service.group_invitations(group_id, invitee_phone) 
    WHERE status = 'PENDING';

