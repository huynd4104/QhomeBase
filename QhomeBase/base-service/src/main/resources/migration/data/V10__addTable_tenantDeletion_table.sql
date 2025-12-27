
CREATE EXTENSION IF NOT EXISTS pgcrypto;


DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = 'data') THEN
    EXECUTE 'CREATE SCHEMA data';
END IF;
END$$;


DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_type t
    JOIN pg_namespace n ON n.oid = t.typnamespace
    WHERE t.typname = 'tenant_deletion_status'
      AND n.nspname = 'data'
  ) THEN
    EXECUTE 'CREATE TYPE data.tenant_deletion_status AS ENUM (''PENDING'',''APPROVED'',''REJECTED'',''CANCELED'')';
END IF;
END$$;


CREATE TABLE IF NOT EXISTS data.tenant_deletion_requests (
                                                             id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID NOT NULL,
    requested_by UUID NOT NULL,
    reason       TEXT,
    approved_by  UUID NULL,
    note         TEXT,
    status       data.tenant_deletion_status NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    approved_at  TIMESTAMPTZ NULL
    );


CREATE INDEX IF NOT EXISTS ix_tdr_tenant_status
    ON data.tenant_deletion_requests (tenant_id, status);
