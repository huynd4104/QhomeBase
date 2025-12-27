ALTER TABLE data.Buildings
    ADD COLUMN IF NOT EXISTS is_deleted  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS status      TEXT    NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS created_by  TEXT    NOT NULL DEFAULT 'system',
    ADD COLUMN IF NOT EXISTS updated_by  TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS uq_buildings_tenant_code_active_ci
    ON data.Buildings (tenant_id, lower(code))
    WHERE is_deleted = FALSE;
