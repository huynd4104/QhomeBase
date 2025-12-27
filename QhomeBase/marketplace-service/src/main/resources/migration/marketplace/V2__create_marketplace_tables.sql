-- Create marketplace_posts table
CREATE TABLE IF NOT EXISTS marketplace.marketplace_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resident_id UUID NOT NULL,
    building_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    price NUMERIC(15,2),
    category VARCHAR(50) NOT NULL,
    status marketplace.post_status NOT NULL DEFAULT 'ACTIVE',
    contact_info JSONB,
    location VARCHAR(200),
    view_count BIGINT NOT NULL DEFAULT 0,
    like_count BIGINT NOT NULL DEFAULT 0,
    comment_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Create indexes for marketplace_posts
CREATE INDEX IF NOT EXISTS idx_posts_building ON marketplace.marketplace_posts(building_id);
CREATE INDEX IF NOT EXISTS idx_posts_category ON marketplace.marketplace_posts(category);
CREATE INDEX IF NOT EXISTS idx_posts_status ON marketplace.marketplace_posts(status);
CREATE INDEX IF NOT EXISTS idx_posts_created ON marketplace.marketplace_posts(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_posts_resident ON marketplace.marketplace_posts(resident_id);
CREATE INDEX IF NOT EXISTS idx_posts_price ON marketplace.marketplace_posts(price) WHERE price IS NOT NULL;

-- Create marketplace_post_images table
CREATE TABLE IF NOT EXISTS marketplace.marketplace_post_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES marketplace.marketplace_posts(id) ON DELETE CASCADE,
    image_url TEXT NOT NULL,
    thumbnail_url TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_images_post ON marketplace.marketplace_post_images(post_id);

-- Create marketplace_likes table
CREATE TABLE IF NOT EXISTS marketplace.marketplace_likes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES marketplace.marketplace_posts(id) ON DELETE CASCADE,
    resident_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_likes_post_resident UNIQUE (post_id, resident_id)
);

CREATE INDEX IF NOT EXISTS idx_likes_post ON marketplace.marketplace_likes(post_id);
CREATE INDEX IF NOT EXISTS idx_likes_resident ON marketplace.marketplace_likes(resident_id);

-- Create marketplace_comments table
CREATE TABLE IF NOT EXISTS marketplace.marketplace_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES marketplace.marketplace_posts(id) ON DELETE CASCADE,
    resident_id UUID NOT NULL,
    parent_comment_id UUID REFERENCES marketplace.marketplace_comments(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_comments_post ON marketplace.marketplace_comments(post_id);
CREATE INDEX IF NOT EXISTS idx_comments_resident ON marketplace.marketplace_comments(resident_id);
CREATE INDEX IF NOT EXISTS idx_comments_parent ON marketplace.marketplace_comments(parent_comment_id);

-- Create marketplace_categories table
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

CREATE INDEX IF NOT EXISTS idx_categories_active ON marketplace.marketplace_categories(active, display_order);

-- Insert default categories
INSERT INTO marketplace.marketplace_categories (code, name, name_en, display_order, active) VALUES
    ('ELECTRONICS', 'Đồ điện tử', 'Electronics', 1, true),
    ('FURNITURE', 'Đồ nội thất', 'Furniture', 2, true),
    ('CLOTHING', 'Quần áo', 'Clothing', 3, true),
    ('BOOKS', 'Sách', 'Books', 4, true),
    ('APPLIANCES', 'Đồ gia dụng', 'Appliances', 5, true),
    ('SPORTS', 'Thể thao', 'Sports', 6, true),
    ('TOYS', 'Đồ chơi', 'Toys', 7, true),
    ('OTHER', 'Khác', 'Other', 99, true)
ON CONFLICT (code) DO NOTHING;

