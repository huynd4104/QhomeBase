
-- 1. EXTENSIONS & SCHEMA
CREATE EXTENSION IF NOT EXISTS pgcrypto; --automatic generation of UUIDs
CREATE EXTENSION IF NOT EXISTS citext; --không phân biệt hoa thường
CREATE SCHEMA IF NOT EXISTS iam;

-- 2. TABLES

-- Bảng lưu trữ thông tin người dùng
CREATE TABLE IF NOT EXISTS iam.users (
                                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username CITEXT NOT NULL UNIQUE,
    email CITEXT NOT NULL UNIQUE,
    phone VARCHAR(20) UNIQUE,
    password_hash TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMPTZ,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    reset_otp TEXT,
    otp_expiry TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_users_failed_attempts_nonneg CHECK (failed_attempts >= 0)
    );

-- Bảng định nghĩa các vai trò (Roles)
CREATE TABLE IF NOT EXISTS iam.roles (
                                         role TEXT PRIMARY KEY, -- Lưu dạng UPPERCASE (ADMIN, TECHNICIAN...)
                                         description TEXT,
                                         created_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );
COMMENT ON COLUMN iam.roles.role IS 'Role name in UPPERCASE (matches UserRole enum: ADMIN, ACCOUNTANT, TECHNICIAN, SUPPORTER, RESIDENT, UNIT_OWNER)';

-- Bảng gán vai trò cho người dùng
CREATE TABLE IF NOT EXISTS iam.user_roles (
                                              user_id UUID NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
    role TEXT NOT NULL REFERENCES iam.roles(role) ON DELETE RESTRICT,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    granted_by TEXT,
    PRIMARY KEY (user_id, role)
    );

-- Bảng định nghĩa danh mục quyền (Permissions)
CREATE TABLE IF NOT EXISTS iam.permissions (
                                               code TEXT PRIMARY KEY,
                                               description TEXT
);

-- Bảng ánh xạ Role với Permission
CREATE TABLE IF NOT EXISTS iam.role_permissions (
                                                    role TEXT NOT NULL REFERENCES iam.roles(role) ON DELETE CASCADE,
    permission_code TEXT NOT NULL REFERENCES iam.permissions(code) ON DELETE RESTRICT,
    PRIMARY KEY (role, permission_code)
    );

-- Bảng quản lý Refresh Token (hỗ trợ Token Rotation)
CREATE TABLE IF NOT EXISTS iam.refresh_tokens (
                                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
    token_hash TEXT NOT NULL,
    family_id UUID NOT NULL,
    jti UUID NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMPTZ,
    revocation_reason TEXT,
    created_ip INET,
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_refresh_exp_after_create CHECK (expires_at > created_at),
    UNIQUE (family_id, jti)
    );

-- Bảng quản lý khóa JWKS (cho JWT)
-- Lưu trữ cặp khoá public/private key. Spring boot sẽ dùng các khoá này để ký tên vào JWT, giúp microservices khác xác thực được token mà không cần truy cập iam database
CREATE TABLE IF NOT EXISTS iam.jwks_keys (
                                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    kid TEXT NOT NULL UNIQUE,
    alg TEXT NOT NULL DEFAULT 'RS256',
    public_pem TEXT NOT NULL,
    private_pem_enc BYTEA,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    not_before TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    rotated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by TEXT,
    CONSTRAINT ck_jwks_time_window CHECK (
(not_before IS NULL OR expires_at IS NULL) OR (not_before < expires_at)
    )
    );

-- Bảng nhật ký Audit gán quyền
CREATE TABLE IF NOT EXISTS iam.role_assignment_audit (
                                                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL, -- Building/Tenant context
    role_name TEXT NOT NULL,
    action TEXT NOT NULL CHECK (action IN ('ASSIGN', 'REMOVE')),
    performed_by TEXT NOT NULL,
    reason TEXT,
    performed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    metadata JSONB
    );

-- Bảng nhật ký sự kiện bảo mật (Auth Events)
CREATE TABLE IF NOT EXISTS iam.auth_events (
                                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    username TEXT,
    kind TEXT NOT NULL,
    ip INET,
    user_agent TEXT,
    detail TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

-- 3. INDEXES OPTIMIZATION
CREATE INDEX IF NOT EXISTS ix_users_email ON iam.users(email);
CREATE INDEX IF NOT EXISTS ix_users_username ON iam.users(username);
CREATE INDEX IF NOT EXISTS idx_users_phone ON iam.users(phone);
CREATE INDEX IF NOT EXISTS ix_user_roles_user ON iam.user_roles(user_id);
CREATE INDEX IF NOT EXISTS ix_refresh_tokens_user ON iam.refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS ix_refresh_tokens_expires_at ON iam.refresh_tokens(expires_at);
CREATE INDEX IF NOT EXISTS ix_auth_events_user_time ON iam.auth_events(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_auth_events_kind_time ON iam.auth_events(kind, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_role_assignment_audit_user ON iam.role_assignment_audit(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_jwks_keys_active_one ON iam.jwks_keys(is_active) WHERE is_active = TRUE;