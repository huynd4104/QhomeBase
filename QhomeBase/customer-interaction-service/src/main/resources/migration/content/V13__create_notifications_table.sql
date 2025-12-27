CREATE SCHEMA IF NOT EXISTS content;

DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type t WHERE t.typname='notification_status' AND t.typnamespace=to_regnamespace('content')) THEN
            EXECUTE 'CREATE TYPE content.notification_status AS ENUM (''UNREAD'',''READ'')';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_type t WHERE t.typname='notification_type' AND t.typnamespace=to_regnamespace('content')) THEN
            EXECUTE 'CREATE TYPE content.notification_type AS ENUM (''NEWS'',''REQUEST'',''BILL'',''CONTRACT'',''METER_READING'',''SYSTEM'')';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_type t WHERE t.typname='notification_scope' AND t.typnamespace=to_regnamespace('content')) THEN
            EXECUTE 'CREATE TYPE content.notification_scope AS ENUM (''INTERNAL'',''EXTERNAL'')';
        END IF;
    END$$;

CREATE TABLE IF NOT EXISTS content.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type content.notification_type NOT NULL,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    scope content.notification_scope NOT NULL,
    target_role VARCHAR(50),
    target_building_id UUID,
    status content.notification_status NOT NULL DEFAULT 'UNREAD',
    reference_id UUID,
    reference_type VARCHAR(50),
    action_url TEXT,
    icon_url TEXT,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_notification_internal CHECK (
        (scope = 'INTERNAL' AND target_building_id IS NULL AND target_role IS NOT NULL) OR
        (scope = 'EXTERNAL' AND target_role IS NULL)
    ),
    CONSTRAINT chk_notification_read_at CHECK (
        (status = 'READ' AND read_at IS NOT NULL) OR
        (status = 'UNREAD' AND read_at IS NULL)
    )
);

CREATE INDEX IF NOT EXISTS ix_notifications_scope ON content.notifications (scope);
CREATE INDEX IF NOT EXISTS ix_notifications_target_role ON content.notifications (target_role) WHERE target_role IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_notifications_target_building ON content.notifications (target_building_id) WHERE target_building_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_notifications_type ON content.notifications (type);
CREATE INDEX IF NOT EXISTS ix_notifications_status ON content.notifications (status);
CREATE INDEX IF NOT EXISTS ix_notifications_created_at ON content.notifications (created_at DESC);
CREATE INDEX IF NOT EXISTS ix_notifications_reference ON content.notifications (reference_id, reference_type) WHERE reference_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_notifications_unread ON content.notifications (scope, status, created_at DESC) WHERE status = 'UNREAD';

COMMENT ON TABLE content.notifications IS 'Bảng lưu trữ thông báo cho người dùng';
COMMENT ON COLUMN content.notifications.type IS 'Loại thông báo: NEWS, REQUEST, BILL, CONTRACT, METER_READING, SYSTEM';
COMMENT ON COLUMN content.notifications.scope IS 'Phạm vi: INTERNAL (nội bộ - staff), EXTERNAL (bên ngoài - residents)';
COMMENT ON COLUMN content.notifications.target_role IS 'Role nhận thông báo (INTERNAL only): ALL, ADMIN, TECHNICIAN, SUPPORTER, ACCOUNT, TENANT_OWNER';
COMMENT ON COLUMN content.notifications.target_building_id IS 'Building ID nhận thông báo (EXTERNAL only): NULL = ALL buildings, UUID = building cụ thể';
COMMENT ON COLUMN content.notifications.reference_id IS 'ID của đối tượng liên quan (news_id, request_id, bill_id, etc.)';
COMMENT ON COLUMN content.notifications.reference_type IS 'Loại đối tượng liên quan (NEWS, REQUEST, BILL, etc.)';
COMMENT ON COLUMN content.notifications.action_url IS 'URL để xử lý khi click vào thông báo';
COMMENT ON COLUMN content.notifications.icon_url IS 'URL icon hiển thị cho thông báo';

