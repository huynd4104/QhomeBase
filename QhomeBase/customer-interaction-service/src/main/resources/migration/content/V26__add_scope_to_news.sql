DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type t WHERE t.typname='notification_scope' AND t.typnamespace=to_regnamespace('content')) THEN
            EXECUTE 'CREATE TYPE content.notification_scope AS ENUM (''INTERNAL'',''EXTERNAL'')';
        END IF;
    END$$;

ALTER TABLE content.news
ADD COLUMN IF NOT EXISTS scope content.notification_scope,
ADD COLUMN IF NOT EXISTS target_role VARCHAR(50),
ADD COLUMN IF NOT EXISTS target_building_id UUID;

CREATE INDEX IF NOT EXISTS ix_news_scope ON content.news (scope) WHERE scope IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_news_target_role ON content.news (target_role) WHERE target_role IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_news_target_building ON content.news (target_building_id) WHERE target_building_id IS NOT NULL;

ALTER TABLE content.news
ADD CONSTRAINT chk_news_scope CHECK (
    (scope IS NULL) OR
    (scope = 'INTERNAL' AND target_building_id IS NULL AND target_role IS NOT NULL) OR
    (scope = 'EXTERNAL' AND target_role IS NULL)
);

COMMENT ON COLUMN content.news.scope IS 'Phạm vi: INTERNAL (nội bộ - staff), EXTERNAL (bên ngoài - residents)';
COMMENT ON COLUMN content.news.target_role IS 'Role nhận news (INTERNAL only): ALL, ADMIN, TECHNICIAN, SUPPORTER, ACCOUNT, TENANT_OWNER';
COMMENT ON COLUMN content.news.target_building_id IS 'Building ID nhận news (EXTERNAL only): NULL = ALL buildings, UUID = building cụ thể';

