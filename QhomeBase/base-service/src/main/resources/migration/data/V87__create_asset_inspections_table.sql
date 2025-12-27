-- V87: Create asset_inspections table for checklist when contract expires

-- Create inspection_status enum
DO $$ BEGIN
    CREATE TYPE data.inspection_status AS ENUM (
        'PENDING',
        'IN_PROGRESS',
        'COMPLETED',
        'CANCELLED'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Create asset_inspections table first
CREATE TABLE IF NOT EXISTS data.asset_inspections (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id         UUID NOT NULL, -- Reference to contract in data-docs-service
    unit_id             UUID NOT NULL REFERENCES data.units(id) ON DELETE CASCADE,
    inspection_date     DATE NOT NULL,
    status              data.inspection_status NOT NULL DEFAULT 'PENDING',
    inspector_name      TEXT,
    inspector_notes     TEXT,
    completed_at        TIMESTAMPTZ,
    completed_by        UUID,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          UUID,
    CONSTRAINT uq_asset_inspection_contract UNIQUE (contract_id)
);

-- Create asset_inspection_items table to store checklist items
CREATE TABLE IF NOT EXISTS data.asset_inspection_items (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inspection_id       UUID NOT NULL REFERENCES data.asset_inspections(id) ON DELETE CASCADE,
    asset_id            UUID NOT NULL REFERENCES data.assets(id) ON DELETE CASCADE,
    condition_status    TEXT, -- GOOD, DAMAGED, MISSING, etc.
    notes               TEXT,
    checked             BOOLEAN NOT NULL DEFAULT FALSE,
    checked_at          TIMESTAMPTZ,
    checked_by          UUID,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_asset_inspections_contract ON data.asset_inspections (contract_id);
CREATE INDEX IF NOT EXISTS idx_asset_inspections_unit ON data.asset_inspections (unit_id);
CREATE INDEX IF NOT EXISTS idx_asset_inspections_status ON data.asset_inspections (status);
CREATE INDEX IF NOT EXISTS idx_asset_inspection_items_inspection ON data.asset_inspection_items (inspection_id);
CREATE INDEX IF NOT EXISTS idx_asset_inspection_items_asset ON data.asset_inspection_items (asset_id);

-- Add comments
COMMENT ON TABLE data.asset_inspections IS 'Kiểm tra thiết bị khi hợp đồng hết hạn';
COMMENT ON COLUMN data.asset_inspections.contract_id IS 'ID hợp đồng (từ data-docs-service)';
COMMENT ON COLUMN data.asset_inspections.unit_id IS 'ID căn hộ';
COMMENT ON COLUMN data.asset_inspections.inspection_date IS 'Ngày kiểm tra';
COMMENT ON COLUMN data.asset_inspections.status IS 'Trạng thái kiểm tra';
COMMENT ON COLUMN data.asset_inspections.inspector_name IS 'Tên người kiểm tra';
COMMENT ON COLUMN data.asset_inspections.inspector_notes IS 'Ghi chú của người kiểm tra';

COMMENT ON TABLE data.asset_inspection_items IS 'Danh sách thiết bị cần kiểm tra';
COMMENT ON COLUMN data.asset_inspection_items.asset_id IS 'ID thiết bị cần kiểm tra';
COMMENT ON COLUMN data.asset_inspection_items.condition_status IS 'Tình trạng thiết bị (GOOD, DAMAGED, MISSING, etc.)';
COMMENT ON COLUMN data.asset_inspection_items.checked IS 'Đã kiểm tra chưa';
COMMENT ON COLUMN data.asset_inspection_items.checked_at IS 'Thời gian kiểm tra';
COMMENT ON COLUMN data.asset_inspection_items.checked_by IS 'Người kiểm tra';



