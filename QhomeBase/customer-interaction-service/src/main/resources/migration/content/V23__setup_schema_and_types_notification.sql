CREATE SCHEMA IF NOT EXISTS content;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type t WHERE t.typname='news_status' AND t.typnamespace=to_regnamespace('content')) THEN
            EXECUTE 'CREATE TYPE content.news_status AS ENUM (''DRAFT'',''SCHEDULED'',''PUBLISHED'',''HIDDEN'',''EXPIRED'',''ARCHIVED'')';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_type t WHERE t.typname='viewer_type' AND t.typnamespace=to_regnamespace('content')) THEN
            EXECUTE 'CREATE TYPE content.viewer_type AS ENUM (''RESIDENT'',''USER'')';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_type t WHERE t.typname='target_type' AND t.typnamespace=to_regnamespace('content')) THEN
            EXECUTE 'CREATE TYPE content.target_type AS ENUM (''ALL'',''BUILDING'')';
        END IF;
    END$$;

CREATE TABLE IF NOT EXISTS content.news (
                                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                            tenant_id UUID NOT NULL,
                                            title TEXT NOT NULL,
                                            summary TEXT,
                                            body_html TEXT NOT NULL,
                                            cover_image_url TEXT,
                                            status content.news_status NOT NULL DEFAULT 'DRAFT',
                                            publish_at TIMESTAMPTZ,
                                            expire_at TIMESTAMPTZ,
                                            display_order INTEGER NOT NULL DEFAULT 0,
                                            view_count BIGINT NOT NULL DEFAULT 0,
                                            created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                            created_by UUID,
                                            updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                            updated_by UUID,
                                            CONSTRAINT chk_news_time_window CHECK (expire_at IS NULL OR publish_at IS NULL OR expire_at > publish_at),
                                            CONSTRAINT chk_display_order_nonneg CHECK (display_order >= 0)
);

CREATE INDEX IF NOT EXISTS ix_news_tenant_status         ON content.news (tenant_id, status);
CREATE INDEX IF NOT EXISTS ix_news_publish_expire        ON content.news (tenant_id, COALESCE(publish_at, created_at), expire_at);
CREATE INDEX IF NOT EXISTS ix_news_order                 ON content.news (tenant_id, display_order DESC, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_news_tenant_status_publish ON content.news (tenant_id, status, publish_at);
CREATE INDEX IF NOT EXISTS ix_news_title_lower           ON content.news (lower(title));
CREATE INDEX IF NOT EXISTS ix_news_summary_lower         ON content.news (lower(summary));

CREATE TABLE IF NOT EXISTS content.news_targets (
                                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                    news_id UUID NOT NULL,
                                                    tenant_id UUID NOT NULL,
                                                    target_type content.target_type NOT NULL,
                                                    building_id UUID,
                                                    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                    CONSTRAINT chk_all_requires_null_building CHECK (
                                                        (target_type='ALL' AND building_id IS NULL) OR
                                                        (target_type='BUILDING' AND building_id IS NOT NULL)
                                                        )
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_news_targets_building_once ON content.news_targets (news_id, building_id) WHERE target_type='BUILDING';
CREATE UNIQUE INDEX IF NOT EXISTS uq_news_targets_all_once      ON content.news_targets (news_id)            WHERE target_type='ALL';
CREATE INDEX IF NOT EXISTS ix_news_targets_tenant_type ON content.news_targets (tenant_id, target_type);
CREATE INDEX IF NOT EXISTS ix_news_targets_building    ON content.news_targets (tenant_id, building_id);

CREATE TABLE IF NOT EXISTS content.news_images (
                                                   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                   news_id UUID NOT NULL,
                                                   url TEXT NOT NULL,
                                                   caption TEXT,
                                                   sort_order INTEGER NOT NULL DEFAULT 0,
                                                   created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS ix_news_images_sort ON content.news_images (news_id, sort_order);

CREATE TABLE IF NOT EXISTS content.news_views (
                                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                  news_id UUID NOT NULL,
                                                  tenant_id UUID NOT NULL,
                                                  viewer_type content.viewer_type NOT NULL DEFAULT 'RESIDENT',
                                                  resident_id UUID,
                                                  user_id UUID,
                                                  viewed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                  CONSTRAINT chk_news_views_xor CHECK (
                                                      (resident_id IS NOT NULL AND user_id IS NULL) OR
                                                      (resident_id IS NULL AND user_id IS NOT NULL)
                                                      )
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_news_views_resident ON content.news_views (news_id, resident_id) WHERE resident_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_news_views_user     ON content.news_views (news_id, user_id)     WHERE user_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_news_views_lookup   ON content.news_views (tenant_id, news_id, viewed_at DESC);
CREATE INDEX IF NOT EXISTS ix_news_views_resident ON content.news_views (resident_id, news_id) WHERE resident_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_news_views_user     ON content.news_views (user_id, news_id)     WHERE user_id IS NOT NULL;

CREATE OR REPLACE VIEW content.v_news_active AS
SELECT n.*
FROM content.news n
WHERE n.status IN ('PUBLISHED','SCHEDULED')
  AND (n.publish_at IS NULL OR n.publish_at <= now())
  AND (n.expire_at IS NULL OR n.expire_at > now());
