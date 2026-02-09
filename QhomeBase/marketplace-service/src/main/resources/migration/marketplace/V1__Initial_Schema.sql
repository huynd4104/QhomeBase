/*
 * V1__Initial_Schema.sql
 *
 * Initial database schema for Marketplace Service.
 * This file consolidates previous migrations to provide a clean starting state.
 *
 * Features:
 * - Marketplace Posts, Comments, Likes, Images
 * - Categories
 *
 */

CREATE SCHEMA IF NOT EXISTS marketplace;

-- =================================================================================================
-- 1. Enums and Configurations
-- =================================================================================================

-- Create types if they don't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'post_status') THEN
        CREATE TYPE marketplace.post_status AS ENUM ('ACTIVE', 'SOLD', 'HIDDEN', 'DELETED', 'PENDING_APPROVAL');
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'post_scope') THEN
        CREATE TYPE marketplace.post_scope AS ENUM ('BUILDING', 'NEIGHBORHOOD', 'PUBLIC');
    END IF;
END$$;


-- =================================================================================================
-- 2. Master Data Tables
-- =================================================================================================

-- Table: marketplace.marketplace_categories
-- Description: Categories for marketplace posts.
CREATE TABLE IF NOT EXISTS marketplace.marketplace_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    name_en VARCHAR(100),
    icon VARCHAR(100),
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- =================================================================================================
-- 3. Main Entity Tables
-- =================================================================================================

-- Table: marketplace.marketplace_posts
-- Description: User posts for selling/renting items or services.
CREATE TABLE IF NOT EXISTS marketplace.marketplace_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resident_id UUID NOT NULL,
    building_id UUID NOT NULL,
    category VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    price NUMERIC(15,2),
    location VARCHAR(200),
    contact_info JSONB,
    video_url TEXT,
    
    -- Status & Scope
    status marketplace.post_status NOT NULL DEFAULT 'ACTIVE',
    scope marketplace.post_scope NOT NULL DEFAULT 'BUILDING',
    
    -- Counters (De-normalized for performance)
    view_count BIGINT NOT NULL DEFAULT 0,
    like_count BIGINT NOT NULL DEFAULT 0,
    comment_count BIGINT NOT NULL DEFAULT 0,
    
    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_marketplace_posts_resident_id ON marketplace.marketplace_posts(resident_id);
CREATE INDEX IF NOT EXISTS idx_marketplace_posts_building_id ON marketplace.marketplace_posts(building_id);
CREATE INDEX IF NOT EXISTS idx_marketplace_posts_category ON marketplace.marketplace_posts(category);
CREATE INDEX IF NOT EXISTS idx_marketplace_posts_status ON marketplace.marketplace_posts(status);
CREATE INDEX IF NOT EXISTS idx_marketplace_posts_created_at ON marketplace.marketplace_posts(created_at DESC);


-- Table: marketplace.marketplace_post_images
-- Description: Images associated with uploads.
CREATE TABLE IF NOT EXISTS marketplace.marketplace_post_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL,
    image_url TEXT NOT NULL,
    thumbnail_url TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT fk_post_images_post FOREIGN KEY (post_id) REFERENCES marketplace.marketplace_posts(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_marketplace_post_images_post_id ON marketplace.marketplace_post_images(post_id);


-- Table: marketplace.marketplace_comments
-- Description: Comments on posts.
CREATE TABLE IF NOT EXISTS marketplace.marketplace_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL,
    resident_id UUID NOT NULL,
    parent_comment_id UUID, -- For threaded comments
    content TEXT,
    image_url TEXT,
    video_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES marketplace.marketplace_posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_comment_id) REFERENCES marketplace.marketplace_comments(id) ON DELETE CASCADE,
    CONSTRAINT chk_content_or_media CHECK (content IS NOT NULL OR image_url IS NOT NULL OR video_url IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS idx_marketplace_comments_post_id ON marketplace.marketplace_comments(post_id);
CREATE INDEX IF NOT EXISTS idx_marketplace_comments_parent_id ON marketplace.marketplace_comments(parent_comment_id);


-- Table: marketplace.marketplace_likes
-- Description: Likes on posts.
CREATE TABLE IF NOT EXISTS marketplace.marketplace_likes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL,
    resident_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT fk_likes_post FOREIGN KEY (post_id) REFERENCES marketplace.marketplace_posts(id) ON DELETE CASCADE,
    CONSTRAINT uq_likes_post_resident UNIQUE (post_id, resident_id)
);

CREATE INDEX IF NOT EXISTS idx_marketplace_likes_post_id ON marketplace.marketplace_likes(post_id);
