-- V91: Add damage cost fields to asset inspections for billing

-- Add damage_cost column to asset_inspection_items
ALTER TABLE data.asset_inspection_items
ADD COLUMN IF NOT EXISTS damage_cost NUMERIC(15, 2) DEFAULT 0;

-- Add total_damage_cost column to asset_inspections
ALTER TABLE data.asset_inspections
ADD COLUMN IF NOT EXISTS total_damage_cost NUMERIC(15, 2) DEFAULT 0;

-- Add invoice_id column to link to finance-billing-service
ALTER TABLE data.asset_inspections
ADD COLUMN IF NOT EXISTS invoice_id UUID;

-- Create index for invoice_id
CREATE INDEX IF NOT EXISTS idx_asset_inspections_invoice ON data.asset_inspections (invoice_id);

-- Add comments
COMMENT ON COLUMN data.asset_inspection_items.damage_cost IS 'Tiền thiệt hại cho từng thiết bị (0 nếu GOOD, giá trị thiệt hại nếu DAMAGED, toàn bộ giá trị nếu MISSING)';
COMMENT ON COLUMN data.asset_inspections.total_damage_cost IS 'Tổng tiền thiệt hại của tất cả thiết bị';
COMMENT ON COLUMN data.asset_inspections.invoice_id IS 'ID hóa đơn đã xuất từ finance-billing-service';

