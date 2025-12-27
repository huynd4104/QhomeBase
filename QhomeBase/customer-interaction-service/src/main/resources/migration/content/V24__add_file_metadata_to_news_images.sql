ALTER TABLE content.news_images
ADD COLUMN IF NOT EXISTS file_size BIGINT,
ADD COLUMN IF NOT EXISTS content_type VARCHAR(100);

COMMENT ON COLUMN content.news_images.file_size IS 'File size in bytes';
COMMENT ON COLUMN content.news_images.content_type IS 'MIME type: image/jpeg, image/png, etc.';






