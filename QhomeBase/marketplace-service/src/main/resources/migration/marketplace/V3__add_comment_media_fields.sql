-- Add image_url and video_url fields to marketplace_comments table
ALTER TABLE marketplace.marketplace_comments
ADD COLUMN IF NOT EXISTS image_url TEXT,
ADD COLUMN IF NOT EXISTS video_url TEXT;

COMMENT ON COLUMN marketplace.marketplace_comments.image_url IS 'URL of image attached to comment';
COMMENT ON COLUMN marketplace.marketplace_comments.video_url IS 'URL of video attached to comment';

