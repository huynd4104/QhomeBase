CREATE SCHEMA IF NOT EXISTS iam;

-- Ensure users table exists with full structure
-- Create table if not exists, or alter if it was created with minimal structure (from base-service V47)
CREATE TABLE IF NOT EXISTS iam.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Add columns if they don't exist (in case table was created with minimal structure)
DO $$
BEGIN
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

-- Ensure roles table exists (should be created by V1, but create if missing for database clones)
CREATE TABLE IF NOT EXISTS iam.roles (
    role TEXT PRIMARY KEY,
    description TEXT,
    is_global BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_roles_code_lower CHECK (role = lower(role))
);

-- Ensure user_roles table exists (should be created by V1, but create if missing for database clones)
CREATE TABLE IF NOT EXISTS iam.user_roles (
    user_id UUID NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
    role TEXT NOT NULL REFERENCES iam.roles(role) ON DELETE RESTRICT,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    granted_by TEXT,
    PRIMARY KEY (user_id, role)
);

CREATE TABLE IF NOT EXISTS iam.permissions (
                                               code TEXT PRIMARY KEY,
                                               description TEXT
);

CREATE TABLE IF NOT EXISTS iam.role_permissions (
                                                    role TEXT NOT NULL REFERENCES iam.roles(role) ON DELETE CASCADE,
    permission_code TEXT NOT NULL REFERENCES iam.permissions(code) ON DELETE RESTRICT,
    PRIMARY KEY (role, permission_code)
    );

CREATE TABLE IF NOT EXISTS iam.tenant_roles (
                                                tenant_id UUID NOT NULL,
                                                role TEXT NOT NULL,
                                                base_role TEXT NULL REFERENCES iam.roles(role),
    description TEXT,
    PRIMARY KEY (tenant_id, role)
    );

CREATE TABLE IF NOT EXISTS iam.tenant_role_permissions (
                                                           tenant_id UUID NOT NULL,
                                                           role TEXT NOT NULL,
                                                           permission_code TEXT NOT NULL REFERENCES iam.permissions(code),
    PRIMARY KEY (tenant_id, role, permission_code),
    FOREIGN KEY (tenant_id, role)
    REFERENCES iam.tenant_roles(tenant_id, role) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS iam.user_tenant_roles (
                                                     user_id UUID NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL,
    role TEXT NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    granted_by TEXT,
    PRIMARY KEY (user_id, tenant_id, role),
    FOREIGN KEY (tenant_id, role)
    REFERENCES iam.tenant_roles(tenant_id, role) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS ix_user_roles_user ON iam.user_roles(user_id);
CREATE INDEX IF NOT EXISTS ix_user_roles_role ON iam.user_roles(role);

CREATE INDEX IF NOT EXISTS ix_role_permissions_role
    ON iam.role_permissions(role);

CREATE INDEX IF NOT EXISTS ix_role_permissions_perm
    ON iam.role_permissions(permission_code);

CREATE INDEX IF NOT EXISTS ix_tenant_role_permissions_perm
    ON iam.tenant_role_permissions(permission_code);

CREATE INDEX IF NOT EXISTS ix_user_tenant_roles_user
    ON iam.user_tenant_roles(user_id);

CREATE INDEX IF NOT EXISTS ix_user_tenant_roles_tenant
    ON iam.user_tenant_roles(tenant_id);
