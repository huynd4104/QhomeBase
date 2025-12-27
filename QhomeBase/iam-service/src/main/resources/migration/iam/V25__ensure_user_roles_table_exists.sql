-- Ensure user_roles table exists
-- This migration ensures the user_roles table exists, as it may not have been created
-- in V1 or V2 due to foreign key constraint issues in new database clones

CREATE SCHEMA IF NOT EXISTS iam;

-- Ensure users table exists first
CREATE TABLE IF NOT EXISTS iam.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Add columns to users table if they don't exist (in case table was created with minimal structure)
DO $$
BEGIN
    -- Add username column if missing
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'iam' AND table_name = 'users' AND column_name = 'username') THEN
        ALTER TABLE iam.users ADD COLUMN username CITEXT;
    END IF;
    
    -- Add unique constraint for username if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_users_username') THEN
        ALTER TABLE iam.users ADD CONSTRAINT uq_users_username UNIQUE (username);
    END IF;
    
    -- Add email column if missing
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'iam' AND table_name = 'users' AND column_name = 'email') THEN
        ALTER TABLE iam.users ADD COLUMN email CITEXT;
    END IF;
    
    -- Add unique constraint for email if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_users_email') THEN
        ALTER TABLE iam.users ADD CONSTRAINT uq_users_email UNIQUE (email);
    END IF;
    
    -- Add password_hash column if missing
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'iam' AND table_name = 'users' AND column_name = 'password_hash') THEN
        ALTER TABLE iam.users ADD COLUMN password_hash TEXT;
    END IF;
    
    -- Add active column if missing
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'iam' AND table_name = 'users' AND column_name = 'active') THEN
        ALTER TABLE iam.users ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
    END IF;
    
    -- Add last_login_at column if missing
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'iam' AND table_name = 'users' AND column_name = 'last_login_at') THEN
        ALTER TABLE iam.users ADD COLUMN last_login_at TIMESTAMPTZ;
    END IF;
    
    -- Add failed_attempts column if missing
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'iam' AND table_name = 'users' AND column_name = 'failed_attempts') THEN
        ALTER TABLE iam.users ADD COLUMN failed_attempts INTEGER NOT NULL DEFAULT 0;
    END IF;
    
    -- Add locked_until column if missing
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'iam' AND table_name = 'users' AND column_name = 'locked_until') THEN
        ALTER TABLE iam.users ADD COLUMN locked_until TIMESTAMPTZ;
    END IF;
    
    -- Add constraint if missing
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_users_failed_attempts_nonneg') THEN
        ALTER TABLE iam.users ADD CONSTRAINT ck_users_failed_attempts_nonneg CHECK (failed_attempts >= 0);
    END IF;
END $$;

-- Ensure roles table exists first
CREATE TABLE IF NOT EXISTS iam.roles (
    role TEXT PRIMARY KEY,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Create user_roles table if it doesn't exist
CREATE TABLE IF NOT EXISTS iam.user_roles (
    user_id UUID NOT NULL,
    role TEXT NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    granted_by TEXT,
    PRIMARY KEY (user_id, role)
);

-- Add foreign key constraints only if they don't exist
DO $$
BEGIN
    -- Add foreign key to users if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'user_roles_user_id_fkey' 
        AND conrelid = 'iam.user_roles'::regclass
    ) THEN
        ALTER TABLE iam.user_roles 
        ADD CONSTRAINT user_roles_user_id_fkey 
        FOREIGN KEY (user_id) REFERENCES iam.users(id) ON DELETE CASCADE;
    END IF;
    
    -- Add foreign key to roles if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'user_roles_role_fkey' 
        AND conrelid = 'iam.user_roles'::regclass
    ) THEN
        ALTER TABLE iam.user_roles 
        ADD CONSTRAINT user_roles_role_fkey 
        FOREIGN KEY (role) REFERENCES iam.roles(role) ON DELETE RESTRICT;
    END IF;
END $$;

-- Create indexes if they don't exist
CREATE INDEX IF NOT EXISTS ix_user_roles_user ON iam.user_roles(user_id);
CREATE INDEX IF NOT EXISTS ix_user_roles_role ON iam.user_roles(role);

