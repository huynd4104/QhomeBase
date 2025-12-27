CREATE TABLE IF NOT EXISTS iam.user_tenant_grants (
    user_id UUID NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL,
    permission_code TEXT NOT NULL,
    expires_at TIMESTAMPTZ NULL,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    granted_by TEXT NOT NULL,
    reason TEXT,
    
    CONSTRAINT uk_user_tenant_grant UNIQUE (user_id, tenant_id, permission_code)
);

CREATE TABLE IF NOT EXISTS iam.user_tenant_denies (
    user_id UUID NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL,
    permission_code TEXT NOT NULL,
    expires_at TIMESTAMPTZ NULL,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    granted_by TEXT NOT NULL,
    reason TEXT,
    
    CONSTRAINT uk_user_tenant_deny UNIQUE (user_id, tenant_id, permission_code)
);

CREATE INDEX IF NOT EXISTS ix_user_tenant_grants_user ON iam.user_tenant_grants(user_id);
CREATE INDEX IF NOT EXISTS ix_user_tenant_grants_tenant ON iam.user_tenant_grants(tenant_id);
CREATE INDEX IF NOT EXISTS ix_user_tenant_grants_permission ON iam.user_tenant_grants(permission_code);
CREATE INDEX IF NOT EXISTS ix_user_tenant_grants_expires ON iam.user_tenant_grants(expires_at);

CREATE INDEX IF NOT EXISTS ix_user_tenant_denies_user ON iam.user_tenant_denies(user_id);
CREATE INDEX IF NOT EXISTS ix_user_tenant_denies_tenant ON iam.user_tenant_denies(tenant_id);
CREATE INDEX IF NOT EXISTS ix_user_tenant_denies_permission ON iam.user_tenant_denies(permission_code);
CREATE INDEX IF NOT EXISTS ix_user_tenant_denies_expires ON iam.user_tenant_denies(expires_at);

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
    user_grants AS (
        SELECT permission_code
        FROM iam.user_tenant_grants
        WHERE user_id = p_user_id AND tenant_id = p_tenant_id 
          AND (expires_at IS NULL OR expires_at > now())
    ),
    user_denies AS (
        SELECT permission_code
        FROM iam.user_tenant_denies
        WHERE user_id = p_user_id AND tenant_id = p_tenant_id
          AND (expires_at IS NULL OR expires_at > now())
    ),
    final_perms AS (
        SELECT DISTINCT permission_code
        FROM (
            SELECT permission_code FROM global_perms
            UNION ALL
            SELECT permission_code FROM tenant_role_perms
            UNION ALL
            SELECT permission_code FROM user_grants
        ) u
        WHERE permission_code NOT IN (
            SELECT permission_code FROM user_denies
        )
    )
    SELECT permission_code FROM final_perms;
END;
$$ LANGUAGE plpgsql;








































