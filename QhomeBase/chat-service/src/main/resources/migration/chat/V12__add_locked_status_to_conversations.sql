-- Migration: V12__add_locked_status_to_conversations.sql
-- Add LOCKED status support for conversations
-- LOCKED: Users are not friends anymore but conversation still exists (can view history, cannot send messages)

-- Update comment for conversations.status column to include LOCKED
COMMENT ON COLUMN chat_service.conversations.status IS 
    'Conversation status: PENDING (invitation sent, not accepted), ACTIVE (friends, can chat), BLOCKED (one user blocked the other), LOCKED (not friends, can view history but cannot send), DELETED (both participants hidden)';
