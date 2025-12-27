-- Create friendships table
CREATE TABLE IF NOT EXISTS chat_service.friendships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user1_id UUID NOT NULL,
    user2_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Ensure user1_id < user2_id for uniqueness
    CONSTRAINT friendships_user1_lt_user2 CHECK (user1_id < user2_id),
    CONSTRAINT friendships_unique_pair UNIQUE (user1_id, user2_id)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_friendships_user1 ON chat_service.friendships(user1_id);
CREATE INDEX IF NOT EXISTS idx_friendships_user2 ON chat_service.friendships(user2_id);
CREATE INDEX IF NOT EXISTS idx_friendships_is_active ON chat_service.friendships(is_active);

-- Add comment
COMMENT ON TABLE chat_service.friendships IS 'Stores friendship relationships between users. Created when direct chat invitation is accepted.';
COMMENT ON COLUMN chat_service.friendships.user1_id IS 'The user with smaller UUID (for consistency)';
COMMENT ON COLUMN chat_service.friendships.user2_id IS 'The user with larger UUID (for consistency)';
COMMENT ON COLUMN chat_service.friendships.is_active IS 'FALSE when one user blocks the other';

