-- Create card_pricing table to store configurable card registration fees
CREATE TABLE IF NOT EXISTS card.card_pricing (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    card_type VARCHAR(50) NOT NULL UNIQUE, -- VEHICLE, RESIDENT, ELEVATOR
    price NUMERIC(14, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    CONSTRAINT chk_card_type CHECK (card_type IN ('VEHICLE', 'RESIDENT', 'ELEVATOR')),
    CONSTRAINT chk_price_positive CHECK (price > 0)
);

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_card_pricing_type_active ON card.card_pricing(card_type, is_active);

-- Insert default pricing (30000 VND for each card type)
INSERT INTO card.card_pricing (card_type, price, currency, description, is_active)
VALUES 
    ('VEHICLE', 30000, 'VND', 'Phí đăng ký thẻ xe', true),
    ('RESIDENT', 30000, 'VND', 'Phí đăng ký thẻ cư dân', true),
    ('ELEVATOR', 30000, 'VND', 'Phí đăng ký thẻ thang máy', true)
ON CONFLICT (card_type) DO NOTHING;

COMMENT ON TABLE card.card_pricing IS 'Bảng lưu giá đăng ký thẻ có thể tùy chỉnh bởi admin';
COMMENT ON COLUMN card.card_pricing.card_type IS 'Loại thẻ: VEHICLE, RESIDENT, ELEVATOR';
COMMENT ON COLUMN card.card_pricing.price IS 'Giá đăng ký thẻ (VND)';
COMMENT ON COLUMN card.card_pricing.is_active IS 'Trạng thái: true = đang áp dụng, false = tạm ngưng';

