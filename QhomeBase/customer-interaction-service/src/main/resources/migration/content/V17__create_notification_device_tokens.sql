CREATE TABLE IF NOT EXISTS content.notification_device_token (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    resident_id UUID,
    building_id UUID,
    role VARCHAR(100),
    token VARCHAR(255) NOT NULL,
    platform VARCHAR(40),
    app_version VARCHAR(40),
    last_seen_at TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    disabled BOOLEAN DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS notification_device_token_token_uindex
    ON content.notification_device_token (token);

CREATE INDEX IF NOT EXISTS notification_device_token_building_idx
    ON content.notification_device_token (building_id)
    WHERE disabled IS FALSE;

CREATE INDEX IF NOT EXISTS notification_device_token_role_idx
    ON content.notification_device_token (role)
    WHERE disabled IS FALSE;

CREATE OR REPLACE FUNCTION content.notification_device_token_touch_updated_at()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS notification_device_token_set_updated_at
    ON content.notification_device_token;

CREATE TRIGGER notification_device_token_set_updated_at
    BEFORE UPDATE ON content.notification_device_token
    FOR EACH ROW
EXECUTE FUNCTION content.notification_device_token_touch_updated_at();

