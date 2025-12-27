-- V9: Remove tenant architecture from customer-interaction-service
-- Remove tenant_id from all content schema tables

-- Drop tenant_id columns
ALTER TABLE IF EXISTS content.news DROP COLUMN IF EXISTS tenant_id CASCADE;
ALTER TABLE IF EXISTS content.news_targets DROP COLUMN IF EXISTS tenant_id CASCADE;
ALTER TABLE IF EXISTS content.news_views DROP COLUMN IF EXISTS tenant_id CASCADE;
ALTER TABLE IF EXISTS content.news_images DROP COLUMN IF EXISTS tenant_id CASCADE;

-- Drop any tenant-related indexes
DROP INDEX IF EXISTS content.idx_news_tenant_status;
DROP INDEX IF EXISTS content.idx_news_tenant_created_at;

