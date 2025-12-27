-- Create enum type for post scope
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_type t
    JOIN pg_namespace n ON n.oid = t.typnamespace
    WHERE t.typname = 'post_scope' AND n.nspname = 'marketplace'
  ) THEN
    CREATE TYPE marketplace.post_scope AS ENUM ('BUILDING', 'ALL', 'BOTH');
  END IF;
END$$;

-- Add scope column to marketplace_posts table
ALTER TABLE marketplace.marketplace_posts
ADD COLUMN IF NOT EXISTS scope marketplace.post_scope NOT NULL DEFAULT 'BUILDING';

-- Add index for scope
CREATE INDEX IF NOT EXISTS idx_posts_scope ON marketplace.marketplace_posts(scope);

COMMENT ON COLUMN marketplace.marketplace_posts.scope IS 'Phạm vi hiển thị: BUILDING (chỉ building của tôi), ALL (tất cả), BOTH (cả 2)';

