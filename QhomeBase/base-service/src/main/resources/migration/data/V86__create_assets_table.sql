-- V86: Create assets table for managing unit assets (air conditioner, kitchen, etc.)

-- Create asset_type enum (check if exists first to avoid errors if migration was run before)
DO $$ BEGIN
    CREATE TYPE data.asset_type AS ENUM (
        'AIR_CONDITIONER',
        'KITCHEN',
        'WATER_HEATER',
        'FURNITURE',
        'OTHER'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Create assets table
CREATE TABLE IF NOT EXISTS data.assets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_id         UUID NOT NULL REFERENCES data.units(id) ON DELETE CASCADE,
    asset_type      data.asset_type NOT NULL,
    asset_code      TEXT NOT NULL,
    name            TEXT,
    brand           TEXT,
    model           TEXT,
    serial_number   TEXT,
    description     TEXT,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    installed_at    DATE,
    removed_at      DATE,
    warranty_until  DATE,
    purchase_price  NUMERIC(14, 2),
    purchase_date   DATE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_asset_code UNIQUE (asset_code),
    CONSTRAINT ck_assets_period CHECK (removed_at IS NULL OR removed_at >= installed_at),
    CONSTRAINT ck_assets_warranty CHECK (warranty_until IS NULL OR warranty_until >= installed_at)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_assets_unit ON data.assets (unit_id);
CREATE INDEX IF NOT EXISTS idx_assets_type ON data.assets (asset_type);
CREATE INDEX IF NOT EXISTS idx_assets_active ON data.assets (active);
CREATE INDEX IF NOT EXISTS idx_assets_code ON data.assets (asset_code);

-- Add comments
COMMENT ON TABLE data.assets IS 'Tài sản của căn hộ (điều hòa, bếp, nóng lạnh, nội thất, khác)';
COMMENT ON COLUMN data.assets.asset_type IS 'Loại tài sản';
COMMENT ON COLUMN data.assets.asset_code IS 'Mã tài sản (unique)';
COMMENT ON COLUMN data.assets.name IS 'Tên tài sản';
COMMENT ON COLUMN data.assets.brand IS 'Thương hiệu';
COMMENT ON COLUMN data.assets.model IS 'Model';
COMMENT ON COLUMN data.assets.serial_number IS 'Số serial';
COMMENT ON COLUMN data.assets.description IS 'Mô tả chi tiết';
COMMENT ON COLUMN data.assets.installed_at IS 'Ngày lắp đặt';
COMMENT ON COLUMN data.assets.removed_at IS 'Ngày tháo gỡ';
COMMENT ON COLUMN data.assets.warranty_until IS 'Bảo hành đến ngày';
COMMENT ON COLUMN data.assets.purchase_price IS 'Giá mua';
COMMENT ON COLUMN data.assets.purchase_date IS 'Ngày mua';


