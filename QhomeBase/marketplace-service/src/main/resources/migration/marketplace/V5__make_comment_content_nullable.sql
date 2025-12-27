-- Make comment content nullable to allow comments with only images/videos
ALTER TABLE marketplace.marketplace_comments
ALTER COLUMN content DROP NOT NULL;

COMMENT ON COLUMN marketplace.marketplace_comments.content IS 'Comment text content. Can be NULL if comment only has image or video.';

