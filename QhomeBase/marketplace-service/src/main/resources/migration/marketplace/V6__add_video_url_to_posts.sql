-- V12: Add video_url column to marketplace_posts table
-- This allows posts to include video stored in data-docs-service VideoStorage

ALTER TABLE marketplace.marketplace_posts 
ADD COLUMN IF NOT EXISTS video_url TEXT;

COMMENT ON COLUMN marketplace.marketplace_posts.video_url IS 
'URL to video stored in data-docs-service VideoStorage. Format: http://host:port/api/videos/stream/{videoId}';
