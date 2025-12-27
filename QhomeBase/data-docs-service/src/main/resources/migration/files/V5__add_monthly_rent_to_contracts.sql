-- Migration: V5__add_monthly_rent_to_contracts.sql
-- Thêm column monthly_rent vào bảng contracts

ALTER TABLE files.contracts
    ADD COLUMN IF NOT EXISTS monthly_rent NUMERIC(14,2);

COMMENT ON COLUMN files.contracts.monthly_rent IS 'Tiền thuê hàng tháng (VND) - chỉ dùng cho hợp đồng RENTAL';







