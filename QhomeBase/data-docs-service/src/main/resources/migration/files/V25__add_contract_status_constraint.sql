-- Migration: V25__add_contract_status_constraint.sql
-- Thêm CHECK constraint cho status field để cho phép 4 giá trị: ACTIVE, INACTIVE, EXPIRED, CANCELLED

-- Drop constraint nếu đã tồn tại (để có thể chạy lại migration)
ALTER TABLE files.contracts
    DROP CONSTRAINT IF EXISTS ck_contracts_status;

-- Thêm CHECK constraint cho status
ALTER TABLE files.contracts
    ADD CONSTRAINT ck_contracts_status 
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'EXPIRED', 'CANCELLED'));

-- Comment
COMMENT ON CONSTRAINT ck_contracts_status ON files.contracts IS 'Chỉ cho phép 4 trạng thái hợp đồng: ACTIVE, INACTIVE, EXPIRED, CANCELLED';

