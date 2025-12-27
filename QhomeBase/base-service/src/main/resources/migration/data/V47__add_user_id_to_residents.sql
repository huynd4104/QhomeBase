-- V47: Add user_id column to residents table to link with iam.users
-- This allows residents to have user accounts for login

-- Create iam schema if it doesn't exist (for new database clones)
CREATE SCHEMA IF NOT EXISTS iam;

-- Create iam.users table if it doesn't exist (for new database clones)
-- This is a minimal structure - iam-service will have the full schema
CREATE TABLE IF NOT EXISTS iam.users (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Add user_id column (nullable, can be null if resident doesn't have account yet)
ALTER TABLE data.residents
    ADD COLUMN IF NOT EXISTS user_id UUID;

-- Add foreign key constraint to iam.users
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_residents_user_id' 
        AND connamespace = 'data'::regnamespace
    ) THEN
        ALTER TABLE data.residents
            ADD CONSTRAINT fk_residents_user_id
            FOREIGN KEY (user_id) REFERENCES iam.users(id)
            ON DELETE SET NULL;
    END IF;
END $$;

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_residents_user_id 
    ON data.residents(user_id) 
    WHERE user_id IS NOT NULL;

-- Add comment
COMMENT ON COLUMN data.residents.user_id IS 'Foreign key to iam.users.id. NULL if resident does not have a user account yet.';


