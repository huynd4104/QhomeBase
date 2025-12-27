-- Create schema for chat service
CREATE SCHEMA IF NOT EXISTS chat_service;

-- Groups table
CREATE TABLE IF NOT EXISTS chat_service.groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_by UUID NOT NULL,
    building_id UUID,
    avatar_url TEXT,
    max_members INTEGER DEFAULT 30,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Group members table
CREATE TABLE IF NOT EXISTS chat_service.group_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL REFERENCES chat_service.groups(id) ON DELETE CASCADE,
    resident_id UUID NOT NULL,
    role VARCHAR(50) DEFAULT 'MEMBER', -- ADMIN, MODERATOR, MEMBER
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_read_at TIMESTAMP WITH TIME ZONE,
    is_muted BOOLEAN DEFAULT false,
    UNIQUE(group_id, resident_id)
);

-- Messages table
CREATE TABLE IF NOT EXISTS chat_service.messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL REFERENCES chat_service.groups(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL,
    content TEXT,
    message_type VARCHAR(50) DEFAULT 'TEXT', -- TEXT, IMAGE, FILE, SYSTEM
    image_url TEXT,
    file_url TEXT,
    file_name VARCHAR(255),
    file_size BIGINT,
    reply_to_message_id UUID REFERENCES chat_service.messages(id) ON DELETE SET NULL,
    is_edited BOOLEAN DEFAULT false,
    is_deleted BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_group_members_group_id ON chat_service.group_members(group_id);
CREATE INDEX IF NOT EXISTS idx_group_members_resident_id ON chat_service.group_members(resident_id);
CREATE INDEX IF NOT EXISTS idx_messages_group_id ON chat_service.messages(group_id);
CREATE INDEX IF NOT EXISTS idx_messages_sender_id ON chat_service.messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_messages_created_at ON chat_service.messages(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_messages_reply_to ON chat_service.messages(reply_to_message_id);
CREATE INDEX IF NOT EXISTS idx_groups_building_id ON chat_service.groups(building_id);
CREATE INDEX IF NOT EXISTS idx_groups_created_by ON chat_service.groups(created_by);

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION chat_service.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers for updated_at
CREATE TRIGGER update_groups_updated_at BEFORE UPDATE ON chat_service.groups
    FOR EACH ROW EXECUTE FUNCTION chat_service.update_updated_at_column();

CREATE TRIGGER update_messages_updated_at BEFORE UPDATE ON chat_service.messages
    FOR EACH ROW EXECUTE FUNCTION chat_service.update_updated_at_column();

