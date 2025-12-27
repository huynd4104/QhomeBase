-- Migration: V26__create_video_storage_table.sql
-- Tạo bảng video_storage để lưu video thay vì ImageKit

CREATE TABLE IF NOT EXISTS files.video_storage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(500) NOT NULL,
    file_path TEXT NOT NULL,
    file_url TEXT NOT NULL,
    content_type VARCHAR(100) NOT NULL DEFAULT 'video/mp4',
    file_size BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL, -- 'repair_request', 'marketplace_post', 'direct_chat', 'group_chat', 'marketplace_comment'
    owner_id UUID, -- ID của entity sở hữu video (post_id, conversation_id, group_id, request_id)
    resolution VARCHAR(20), -- '480p', '720p', '1080p', etc.
    duration_seconds INTEGER, -- Thời lượng video (giây)
    width INTEGER, -- Chiều rộng video (pixels)
    height INTEGER, -- Chiều cao video (pixels)
    uploaded_by UUID NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    
    CONSTRAINT ck_video_storage_file_size CHECK (file_size > 0),
    CONSTRAINT ck_video_storage_duration CHECK (duration_seconds IS NULL OR duration_seconds >= 0)
);

