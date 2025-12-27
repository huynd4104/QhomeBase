-- Convert role_permissions table to uppercase to match user_roles table
-- This migration only runs if there is existing data to convert
-- For new database clones, there is no data to convert, so this migration does nothing

DO $$
DECLARE
    has_lowercase_data BOOLEAN := FALSE;
BEGIN
    -- Check if there's any lowercase data to convert in role_permissions
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'iam' AND table_name = 'role_permissions') THEN
        IF EXISTS (SELECT 1 FROM iam.role_permissions WHERE role != UPPER(role) LIMIT 1) THEN
            has_lowercase_data := TRUE;
        END IF;
    END IF;
    
    -- Check if there's any lowercase data to convert in user_roles (only if table exists)
    IF NOT has_lowercase_data AND EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'iam' AND table_name = 'user_roles') THEN
        IF EXISTS (SELECT 1 FROM iam.user_roles WHERE role != UPPER(role) LIMIT 1) THEN
            has_lowercase_data := TRUE;
        END IF;
    END IF;
    
    -- If there's data to convert, drop the lowercase constraint first (needed to allow uppercase roles)
    IF has_lowercase_data THEN
        IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_roles_code_lower') THEN
            ALTER TABLE iam.roles DROP CONSTRAINT ck_roles_code_lower;
        END IF;
    END IF;
    
    -- Only convert if tables exist and have data
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'iam' AND table_name = 'role_permissions') THEN
        -- Check if there's any data to convert (lowercase roles)
        IF EXISTS (SELECT 1 FROM iam.role_permissions WHERE role != UPPER(role) LIMIT 1) THEN
            -- First, create uppercase roles in roles table if they don't exist
            -- Get description from existing lowercase role
            -- Note: is_global column was removed in V10, so we only insert role and description
            INSERT INTO iam.roles (role, description)
            SELECT DISTINCT 
                UPPER(rp.role) as new_role,
                COALESCE(r.description, UPPER(rp.role)) as description
            FROM iam.role_permissions rp
            LEFT JOIN iam.roles r ON r.role = rp.role
            WHERE rp.role != UPPER(rp.role)
              AND NOT EXISTS (SELECT 1 FROM iam.roles WHERE role = UPPER(rp.role))
            ON CONFLICT (role) DO NOTHING;
            
            -- Then update role_permissions to reference uppercase roles
            UPDATE iam.role_permissions 
            SET role = UPPER(role)
            WHERE role != UPPER(role)
              AND EXISTS (SELECT 1 FROM iam.roles WHERE role = UPPER(iam.role_permissions.role));
        END IF;
    END IF;
    
    -- Also update user_roles if it exists and has lowercase roles
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'iam' AND table_name = 'user_roles') THEN
        IF EXISTS (SELECT 1 FROM iam.user_roles WHERE role != UPPER(role) LIMIT 1) THEN
            -- Ensure uppercase roles exist
            -- Note: is_global column was removed in V10, so we only insert role and description
            INSERT INTO iam.roles (role, description)
            SELECT DISTINCT 
                UPPER(ur.role) as new_role,
                COALESCE(r.description, UPPER(ur.role)) as description
            FROM iam.user_roles ur
            LEFT JOIN iam.roles r ON r.role = ur.role
            WHERE ur.role != UPPER(ur.role)
              AND NOT EXISTS (SELECT 1 FROM iam.roles WHERE role = UPPER(ur.role))
            ON CONFLICT (role) DO NOTHING;
            
            -- Update user_roles
            UPDATE iam.user_roles 
            SET role = UPPER(role)
            WHERE role != UPPER(role)
              AND EXISTS (SELECT 1 FROM iam.roles WHERE role = UPPER(iam.user_roles.role));
        END IF;
    END IF;
END $$;

-- Update comments to reflect uppercase format (only if columns exist)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'iam' AND table_name = 'role_permissions' AND column_name = 'role') THEN
        EXECUTE 'COMMENT ON COLUMN iam.role_permissions.role IS ''Role name in UPPERCASE format (matches UserRole enum name: ADMIN, ACCOUNTANT, TECHNICIAN, SUPPORTER, RESIDENT, UNIT_OWNER)''';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'iam' AND table_name = 'user_roles' AND column_name = 'role') THEN
        EXECUTE 'COMMENT ON COLUMN iam.user_roles.role IS ''Role name in UPPERCASE format (matches UserRole enum name: ADMIN, ACCOUNTANT, TECHNICIAN, SUPPORTER, RESIDENT, UNIT_OWNER)''';
    END IF;
END $$;


