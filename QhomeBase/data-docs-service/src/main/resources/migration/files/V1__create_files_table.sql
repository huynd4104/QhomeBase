CREATE SCHEMA IF NOT EXISTS files;

CREATE TABLE IF NOT EXISTS files.file_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(500) NOT NULL,
    file_path TEXT NOT NULL,
    file_url TEXT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    uploaded_by UUID NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_file_metadata_category ON files.file_metadata(category);
CREATE INDEX idx_file_metadata_uploaded_by ON files.file_metadata(uploaded_by);
CREATE INDEX idx_file_metadata_uploaded_at ON files.file_metadata(uploaded_at);
CREATE INDEX idx_file_metadata_deleted ON files.file_metadata(is_deleted) WHERE is_deleted = false;

