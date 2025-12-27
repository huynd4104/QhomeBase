-- Migration: V8__add_mute_and_hide_conversation.sql
-- Add muteUntil and isHidden columns for mute and hide conversation features

-- Add muteUntil and mutedByUserId to group_members
ALTER TABLE chat_service.group_members
    ADD COLUMN IF NOT EXISTS mute_until TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS muted_by_user_id UUID;

-- Add muteUntil, mutedByUserId, and isHidden to conversation_participants
ALTER TABLE chat_service.conversation_participants
    ADD COLUMN IF NOT EXISTS mute_until TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS muted_by_user_id UUID,
    ADD COLUMN IF NOT EXISTS is_hidden BOOLEAN DEFAULT false,
    ADD COLUMN IF NOT EXISTS hidden_at TIMESTAMP WITH TIME ZONE;

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_group_members_mute_until ON chat_service.group_members(mute_until) WHERE mute_until IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_conversation_participants_mute_until ON chat_service.conversation_participants(mute_until) WHERE mute_until IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_conversation_participants_is_hidden ON chat_service.conversation_participants(is_hidden) WHERE is_hidden = true;

