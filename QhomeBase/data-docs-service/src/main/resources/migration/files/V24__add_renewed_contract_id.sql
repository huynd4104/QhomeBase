-- Migration: V24__add_renewed_contract_id.sql
-- Thêm field renewed_contract_id để đánh dấu hợp đồng nào đã được gia hạn thành công

ALTER TABLE files.contracts 
ADD COLUMN IF NOT EXISTS renewed_contract_id UUID;

-- Thêm foreign key constraint
ALTER TABLE files.contracts
ADD CONSTRAINT fk_contracts_renewed_contract 
FOREIGN KEY (renewed_contract_id) REFERENCES files.contracts(id) ON DELETE SET NULL;

-- Thêm index để tối ưu query
CREATE INDEX IF NOT EXISTS idx_contracts_renewed_contract_id ON files.contracts(renewed_contract_id);

-- Comment
COMMENT ON COLUMN files.contracts.renewed_contract_id IS 'ID của hợp đồng mới được tạo khi gia hạn hợp đồng này thành công. NULL nếu chưa được gia hạn.';
