-- Ensure all required columns exist in iam.users table
-- This migration ensures all columns required by the User entity are present

-- Create citext extension if it doesn't exist (required for username and email columns)
CREATE EXTENSION IF NOT EXISTS citext;

DO $$
BEGIN
    -- Add active column if missing
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'iam' AND table_name = 'users' AND column_name = 'active') THEN
        ALTER TABLE iam.users ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
    END IF;
    
    -- Add username column if missing
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'iam' AND table_name = 'users' AND column_name = 'username') THEN
        ALTER TABLE iam.users ADD COLUMN username CITEXT;
    END IF;
    
    -- Add email column if missing
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'iam' AND table_name = 'users' AND column_name = 'email') THEN
        ALTER TABLE iam.users ADD COLUMN email CITEXT;
    END IF;
    
    -- Add password_hash column if missing
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'iam' AND table_name = 'users' AND column_name = 'password_hash') THEN
        ALTER TABLE iam.users ADD COLUMN password_hash TEXT;
    END IF;
    
    -- Add last_login_at column if missing
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'iam' AND table_name = 'users' AND column_name = 'last_login_at') THEN
        ALTER TABLE iam.users ADD COLUMN last_login_at TIMESTAMPTZ;
    END IF;
    
    -- Add failed_attempts column if missing
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'iam' AND table_name = 'users' AND column_name = 'failed_attempts') THEN
        ALTER TABLE iam.users ADD COLUMN failed_attempts INTEGER NOT NULL DEFAULT 0;
    END IF;
    
    -- Add reset_otp column if missing
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'iam' AND table_name = 'users' AND column_name = 'reset_otp') THEN
        ALTER TABLE iam.users ADD COLUMN reset_otp TEXT;
    END IF;
    
    -- Add otp_expiry column if missing
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'iam' AND table_name = 'users' AND column_name = 'otp_expiry') THEN
        ALTER TABLE iam.users ADD COLUMN otp_expiry TIMESTAMPTZ;
    END IF;
END $$;

