-- Drop news_targets table and related objects

-- This table is replaced by scope/targetRole/targetBuildingId in news table

-- Drop foreign key constraint first
ALTER TABLE IF EXISTS content.news_targets DROP CONSTRAINT IF EXISTS fk_news_targets_news CASCADE;

-- Drop indexes
DROP INDEX IF EXISTS content.uq_news_targets_building_once;
DROP INDEX IF EXISTS content.uq_news_targets_all_once;
DROP INDEX IF EXISTS content.ix_news_targets_tenant_type;
DROP INDEX IF EXISTS content.ix_news_targets_building;

-- Drop table
DROP TABLE IF EXISTS content.news_targets CASCADE;

-- Drop enum type (only if not used elsewhere)
-- Note: Check if target_type enum is used by other tables before dropping
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'content' 
        AND column_name LIKE '%target_type%'
        AND table_name != 'news_targets'
    ) THEN
        DROP TYPE IF EXISTS content.target_type CASCADE;
    END IF;
END$$;

COMMENT ON TABLE content.news IS 'Table uses scope/targetRole/targetBuildingId instead of news_targets table';




