ALTER TABLE files.contracts
    ADD COLUMN IF NOT EXISTS purchase_price NUMERIC(14,2),
    ADD COLUMN IF NOT EXISTS payment_method VARCHAR(50),
    ADD COLUMN IF NOT EXISTS payment_terms TEXT,
    ADD COLUMN IF NOT EXISTS purchase_date DATE;

ALTER TABLE files.contracts
    ADD CONSTRAINT ck_contracts_purchase_price_positive CHECK (purchase_price IS NULL OR purchase_price >= 0),
    ADD CONSTRAINT ck_contracts_type_valid CHECK (contract_type IN ('RENTAL', 'PURCHASE'));

CREATE INDEX idx_contracts_contract_type ON files.contracts(contract_type);
CREATE INDEX idx_contracts_purchase_date ON files.contracts(purchase_date) WHERE purchase_date IS NOT NULL;

COMMENT ON COLUMN files.contracts.purchase_price IS 'Giá mua căn hộ (VND) - chỉ dùng cho hợp đồng PURCHASE';
COMMENT ON COLUMN files.contracts.payment_method IS 'Phương thức thanh toán: CASH, BANK_TRANSFER, INSTALLMENT, etc.';
COMMENT ON COLUMN files.contracts.payment_terms IS 'Điều kiện thanh toán (text)';
COMMENT ON COLUMN files.contracts.purchase_date IS 'Ngày mua - chỉ dùng cho hợp đồng PURCHASE';

