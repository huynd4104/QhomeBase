-- Migration: V14__add_fixed_fee_to_pricing_tiers.sql
-- Thêm phí cố định vào bảng pricing_tiers

-- 1. Thêm cột fixed_fee
ALTER TABLE billing.pricing_tiers
    ADD COLUMN IF NOT EXISTS fixed_fee NUMERIC(14,2);         -- Phí cố định cho bậc này (VND)

-- 2. Thêm constraint cho fixed_fee (phải >= 0)
ALTER TABLE billing.pricing_tiers
    ADD CONSTRAINT ck_fixed_fee_positive CHECK (fixed_fee IS NULL OR fixed_fee >= 0);

-- 3. Comment cho cột fixed_fee
COMMENT ON COLUMN billing.pricing_tiers.fixed_fee IS 
    'Phí cố định áp dụng cho bậc này (VND). Được tính riêng, không phụ thuộc vào usage. 
     Nếu NULL hoặc 0 thì không có phí cố định cho bậc này';

