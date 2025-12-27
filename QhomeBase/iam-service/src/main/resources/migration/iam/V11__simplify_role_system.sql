-- First drop existing tables if they exist
DROP TABLE IF EXISTS iam.tenant_role_permissions CASCADE;
DROP TABLE IF EXISTS iam.tenant_roles CASCADE;

-- Create new tenant_role_permissions table
CREATE TABLE iam.tenant_role_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    role TEXT NOT NULL,
    permission_code TEXT NOT NULL,
    granted BOOLEAN NOT NULL DEFAULT true,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    granted_by TEXT NOT NULL,
    
    CONSTRAINT uk_tenant_role_permission UNIQUE (tenant_id, role, permission_code)
);

CREATE INDEX ix_tenant_role_permissions_tenant ON iam.tenant_role_permissions(tenant_id);
CREATE INDEX ix_tenant_role_permissions_role ON iam.tenant_role_permissions(role);
CREATE INDEX ix_tenant_role_permissions_permission ON iam.tenant_role_permissions(permission_code);
CREATE INDEX ix_tenant_role_permissions_granted ON iam.tenant_role_permissions(granted);

-- Clean up user_tenant_roles - delete records with invalid roles
DELETE FROM iam.user_tenant_roles 
WHERE role NOT IN (
    SELECT role FROM iam.roles
);

CREATE OR REPLACE FUNCTION get_user_permissions_in_tenant(
    p_user_id UUID,
    p_tenant_id UUID
)
RETURNS TABLE(permission_code TEXT) AS $$
BEGIN
    RETURN QUERY
    WITH global_perms AS (
        SELECT rp.permission_code
        FROM iam.user_roles ur
        JOIN iam.role_permissions rp ON ur.role = rp.role
        WHERE ur.user_id = p_user_id
    ),
    tenant_role_perms AS (
        SELECT trp.permission_code
        FROM iam.user_tenant_roles utr
        JOIN iam.tenant_role_permissions trp ON utr.tenant_id = trp.tenant_id AND utr.role = trp.role
        WHERE utr.user_id = p_user_id AND utr.tenant_id = p_tenant_id AND trp.granted = true
    ),
    final_perms AS (
        SELECT DISTINCT permission_code
        FROM (
            SELECT permission_code FROM global_perms
            UNION ALL
            SELECT permission_code FROM tenant_role_perms
        ) u
        WHERE permission_code NOT IN (
            SELECT trp.permission_code
            FROM iam.user_tenant_roles utr
            JOIN iam.tenant_role_permissions trp ON utr.tenant_id = trp.tenant_id AND utr.role = trp.role
            WHERE utr.user_id = p_user_id AND utr.tenant_id = p_tenant_id AND trp.granted = false
        )
    )
    SELECT permission_code FROM final_perms;
END;
$$ LANGUAGE plpgsql;
