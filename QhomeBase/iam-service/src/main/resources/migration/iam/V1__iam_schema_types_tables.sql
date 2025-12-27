CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS citext;
CREATE SCHEMA IF NOT EXISTS iam;

CREATE TABLE IF NOT EXISTS iam.users (
                                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username CITEXT NOT NULL UNIQUE,
    email CITEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMPTZ,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_users_failed_attempts_nonneg CHECK (failed_attempts >= 0)
    );

CREATE TABLE IF NOT EXISTS iam.roles (
                                         role TEXT PRIMARY KEY,
                                         description TEXT,
                                         is_global BOOLEAN NOT NULL DEFAULT TRUE,
                                         created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_roles_code_lower CHECK (role = lower(role))
    );

CREATE TABLE IF NOT EXISTS iam.user_roles (
                                              user_id UUID NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
    role TEXT NOT NULL REFERENCES iam.roles(role) ON DELETE RESTRICT,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    granted_by TEXT,
    PRIMARY KEY (user_id, role)
    );

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

CREATE INDEX IF NOT EXISTS ix_users_email ON iam.users(email);
CREATE INDEX IF NOT EXISTS ix_users_username ON iam.users(username);
CREATE INDEX IF NOT EXISTS ix_user_roles_user ON iam.user_roles(user_id);
CREATE INDEX IF NOT EXISTS ix_user_roles_role ON iam.user_roles(role);
CREATE UNIQUE INDEX IF NOT EXISTS uq_jwks_keys_active_one ON iam.jwks_keys(is_active) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS ix_refresh_tokens_user ON iam.refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS ix_refresh_tokens_expires_at ON iam.refresh_tokens(expires_at);
CREATE INDEX IF NOT EXISTS ix_auth_events_user_time ON iam.auth_events(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_auth_events_kind_time ON iam.auth_events(kind, created_at DESC);



