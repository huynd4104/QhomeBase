ALTER TABLE files.contracts
    DROP COLUMN IF EXISTS deposit;

ALTER TABLE files.contracts
    DROP CONSTRAINT IF EXISTS ck_contracts_deposit_positive;

COMMENT ON TABLE files.contracts IS 'Hợp đồng thuê/mua nhà/căn hộ (đã thanh toán đầy đủ)';

