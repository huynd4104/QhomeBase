-- Update user_roles table to match UserRole enum values
-- Enum values: ADMIN, ACCOUNTANT, TECHNICIAN, SUPPORTER, RESIDENT, UNIT_OWNER
-- getRoleName() returns lowercase: admin, accountant, technician, supporter, resident, unit_owner

-- Step 1: First, insert new roles in iam.roles table to ensure foreign key constraints are satisfied
-- Note: Cannot UPDATE primary key directly, so we INSERT new role and UPDATE references, then DELETE old role
-- Note: is_global column was removed in V10, so we only insert role and description
INSERT INTO iam.roles (role, description) VALUES
    ('accountant', 'Accountant'),
    ('resident', 'Resident'),
    ('unit_owner', 'Unit Owner')
ON CONFLICT (role) DO UPDATE SET
    description = EXCLUDED.description;

-- Step 2: Now update user_roles table (foreign key constraint satisfied - accountant exists)
-- Only update if table exists
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'iam' AND table_name = 'user_roles') THEN
        UPDATE iam.user_roles 
        SET role = 'accountant' 
        WHERE role = 'account';
        
        -- Step 3: Remove 'tenant_owner' roles from user_roles (no longer exists in enum)
        DELETE FROM iam.user_roles 
        WHERE role = 'tenant_owner';
        
        -- Step 6: Verify only valid roles remain in user_roles
        DELETE FROM iam.user_roles 
        WHERE role NOT IN ('admin', 'accountant', 'technician', 'supporter', 'resident', 'unit_owner');
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'iam' AND table_name = 'role_permissions') THEN
        -- Step 4: Update role_permissions table
        UPDATE iam.role_permissions 
        SET role = 'accountant' 
        WHERE role = 'account';
        
        DELETE FROM iam.role_permissions 
        WHERE role = 'tenant_owner';
        
        -- Step 6: Verify only valid roles remain in role_permissions
        DELETE FROM iam.role_permissions 
        WHERE role NOT IN ('admin', 'accountant', 'technician', 'supporter', 'resident', 'unit_owner');
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'iam' AND table_name = 'roles') THEN
        -- Step 5: Clean up roles table - remove invalid roles (tenant_owner, account if still exists)
        DELETE FROM iam.roles 
        WHERE role IN ('tenant_owner', 'account');
    END IF;
END $$;

-- Step 7: Add comments (only if columns exist)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'iam' AND table_name = 'user_roles' AND column_name = 'role') THEN
        EXECUTE 'COMMENT ON COLUMN iam.user_roles.role IS ''Base role name (must match UserRole enum getRoleName() values: admin, accountant, technician, supporter, resident, unit_owner)''';
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'iam' AND table_name = 'role_permissions' AND column_name = 'role') THEN
        EXECUTE 'COMMENT ON COLUMN iam.role_permissions.role IS ''Base role name (must match UserRole enum getRoleName() values: admin, accountant, technician, supporter, resident, unit_owner)''';
    END IF;
END $$;








