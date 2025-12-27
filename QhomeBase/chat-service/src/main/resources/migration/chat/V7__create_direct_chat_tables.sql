-- Direct Chat 1-1 Tables
-- ======================

-- Conversations table: Represents a 1-1 chat between two residents
CREATE TABLE IF NOT EXISTS chat_service.conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    participant1_id UUID NOT NULL, -- First participant (resident_id)
    participant2_id UUID NOT NULL, -- Second participant (resident_id)
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, ACTIVE, BLOCKED, CLOSED
    created_by UUID NOT NULL, -- Who initiated the conversation
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    -- Ensure participant1_id < participant2_id for uniqueness
    CONSTRAINT unique_conversation UNIQUE (participant1_id, participant2_id),
    CONSTRAINT check_participants_order CHECK (participant1_id < participant2_id)
);

-- Direct messages table: Messages in 1-1 conversations
CREATE TABLE IF NOT EXISTS chat_service.direct_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES chat_service.conversations(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL, -- Resident who sent the message
    content TEXT,
    message_type VARCHAR(50) DEFAULT 'TEXT', -- TEXT, IMAGE, AUDIO, FILE, SYSTEM
    image_url TEXT,
    file_url TEXT,
    file_name VARCHAR(255),
    file_size BIGINT,
    mime_type VARCHAR(100),
    reply_to_message_id UUID REFERENCES chat_service.direct_messages(id) ON DELETE SET NULL,
    is_edited BOOLEAN DEFAULT false,
    is_deleted BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Direct invitations table: Invitation to start a 1-1 chat
CREATE TABLE IF NOT EXISTS chat_service.direct_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES chat_service.conversations(id) ON DELETE CASCADE,
    inviter_id UUID NOT NULL, -- Resident who sent the invitation
    invitee_id UUID NOT NULL, -- Resident who receives the invitation
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, ACCEPTED, DECLINED, EXPIRED
    initial_message TEXT, -- First message sent with invitation (optional)
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL, -- Invitation expires after 7 days
    responded_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(conversation_id, inviter_id, invitee_id)
);

-- Conversation participants table: Track read status and metadata
CREATE TABLE IF NOT EXISTS chat_service.conversation_participants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES chat_service.conversations(id) ON DELETE CASCADE,
    resident_id UUID NOT NULL,
    last_read_at TIMESTAMP WITH TIME ZONE, -- Last time this participant read messages
    is_muted BOOLEAN DEFAULT false,
    is_blocked BOOLEAN DEFAULT false, -- If this participant blocked the other
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(conversation_id, resident_id)
);

-- Blocks table: Track who blocked whom
CREATE TABLE IF NOT EXISTS chat_service.blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blocker_id UUID NOT NULL, -- Resident who blocks
    blocked_id UUID NOT NULL, -- Resident who is blocked
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(blocker_id, blocked_id),
    CONSTRAINT check_block_self CHECK (blocker_id != blocked_id)
);

-- Direct chat files table: Metadata for files sent in 1-1 chats
CREATE TABLE IF NOT EXISTS chat_service.direct_chat_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES chat_service.conversations(id) ON DELETE CASCADE,
    message_id UUID NOT NULL REFERENCES chat_service.direct_messages(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(50) NOT NULL, -- IMAGE, AUDIO, VIDEO, DOCUMENT
    mime_type VARCHAR(100),
    file_url TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(message_id)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_conversations_participant1 ON chat_service.conversations(participant1_id);
CREATE INDEX IF NOT EXISTS idx_conversations_participant2 ON chat_service.conversations(participant2_id);
CREATE INDEX IF NOT EXISTS idx_conversations_status ON chat_service.conversations(status);
CREATE INDEX IF NOT EXISTS idx_conversations_updated_at ON chat_service.conversations(updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_direct_messages_conversation_id ON chat_service.direct_messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_direct_messages_sender_id ON chat_service.direct_messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_direct_messages_created_at ON chat_service.direct_messages(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_direct_messages_reply_to ON chat_service.direct_messages(reply_to_message_id);

CREATE INDEX IF NOT EXISTS idx_direct_invitations_conversation_id ON chat_service.direct_invitations(conversation_id);
CREATE INDEX IF NOT EXISTS idx_direct_invitations_inviter_id ON chat_service.direct_invitations(inviter_id);
CREATE INDEX IF NOT EXISTS idx_direct_invitations_invitee_id ON chat_service.direct_invitations(invitee_id);
CREATE INDEX IF NOT EXISTS idx_direct_invitations_status ON chat_service.direct_invitations(status);

CREATE INDEX IF NOT EXISTS idx_conversation_participants_conversation_id ON chat_service.conversation_participants(conversation_id);
CREATE INDEX IF NOT EXISTS idx_conversation_participants_resident_id ON chat_service.conversation_participants(resident_id);

CREATE INDEX IF NOT EXISTS idx_blocks_blocker_id ON chat_service.blocks(blocker_id);
CREATE INDEX IF NOT EXISTS idx_blocks_blocked_id ON chat_service.blocks(blocked_id);

CREATE INDEX IF NOT EXISTS idx_direct_chat_files_conversation_id ON chat_service.direct_chat_files(conversation_id);
CREATE INDEX IF NOT EXISTS idx_direct_chat_files_sender_id ON chat_service.direct_chat_files(sender_id);
CREATE INDEX IF NOT EXISTS idx_direct_chat_files_created_at ON chat_service.direct_chat_files(created_at DESC);

-- Triggers for updated_at
CREATE TRIGGER update_conversations_updated_at BEFORE UPDATE ON chat_service.conversations
    FOR EACH ROW EXECUTE FUNCTION chat_service.update_updated_at_column();

CREATE TRIGGER update_direct_messages_updated_at BEFORE UPDATE ON chat_service.direct_messages
    FOR EACH ROW EXECUTE FUNCTION chat_service.update_updated_at_column();

