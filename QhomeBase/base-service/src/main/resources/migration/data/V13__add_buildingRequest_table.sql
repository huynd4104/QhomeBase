CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_type t
    JOIN pg_namespace n ON n.oid = t.typnamespace
    WHERE t.typname = 'building_deletion_status'
      AND n.nspname = 'data'
  ) THEN
    EXECUTE 'CREATE TYPE data.building_deletion_status AS ENUM (''PENDING'',''APPROVED'',''REJECTED'',''CANCELED'')';
END IF;
END$$;

CREATE TABLE IF NOT EXISTS data.building_deletion_requests (
                                                               id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL,
    building_id   UUID NOT NULL,
    requested_by  UUID NOT NULL,
    reason        TEXT,
    approved_by   UUID NULL,
    note          TEXT,
    status        data.building_deletion_status NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    approved_at   TIMESTAMPTZ NULL
    );

CREATE INDEX IF NOT EXISTS ix_bdr_building_status
    ON data.building_deletion_requests (building_id, status);

CREATE INDEX IF NOT EXISTS ix_bdr_tenant_status
    ON data.building_deletion_requests (tenant_id, status);

CREATE UNIQUE INDEX IF NOT EXISTS uq_bdr_building_pending
    ON data.building_deletion_requests (building_id)
    WHERE status = 'PENDING';

CREATE OR REPLACE VIEW data.v_building_deletion_pending AS
SELECT *
FROM data.building_deletion_requests
WHERE status = 'PENDING';
