-- Migration: V2__create_contracts_and_contract_files_tables.sql
-- Tạo bảng contracts và contract_files để lưu hợp đồng đã ký bằng giấy

-- Tạo bảng contracts
CREATE TABLE IF NOT EXISTS files.contracts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    unit_id UUID NOT NULL,
    contract_number VARCHAR(100) NOT NULL,
    contract_type VARCHAR(50) NOT NULL DEFAULT 'RENTAL',
    start_date DATE NOT NULL,
    end_date DATE,
    notes TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID,
    
    CONSTRAINT uq_contracts_number UNIQUE (contract_number),
    CONSTRAINT ck_contracts_dates CHECK (end_date IS NULL OR start_date <= end_date)
);

-- Tạo bảng contract_files để lưu metadata của files đã upload
CREATE TABLE IF NOT EXISTS files.contract_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(500) NOT NULL,
    file_path TEXT NOT NULL,
    file_url TEXT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INTEGER DEFAULT 0,
    uploaded_by UUID NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    
    CONSTRAINT fk_contract_files_contract FOREIGN KEY (contract_id) REFERENCES files.contracts(id) ON DELETE CASCADE
);

-- Tạo indexes
CREATE INDEX idx_contracts_unit_id ON files.contracts(unit_id);
CREATE INDEX idx_contracts_status ON files.contracts(status);
CREATE INDEX idx_contracts_start_date ON files.contracts(start_date);
CREATE INDEX idx_contracts_end_date ON files.contracts(end_date);
CREATE INDEX idx_contracts_created_at ON files.contracts(created_at);

CREATE INDEX idx_contract_files_contract_id ON files.contract_files(contract_id);
CREATE INDEX idx_contract_files_is_deleted ON files.contract_files(is_deleted) WHERE is_deleted = false;
CREATE INDEX idx_contract_files_is_primary ON files.contract_files(is_primary, contract_id) WHERE is_primary = true;
CREATE INDEX idx_contract_files_display_order ON files.contract_files(contract_id, display_order);

-- Comments
COMMENT ON TABLE files.contracts IS 'Hợp đồng thuê nhà/căn hộ';
COMMENT ON COLUMN files.contracts.unit_id IS 'ID của căn hộ/đơn vị (tham chiếu tới data.units)';
COMMENT ON COLUMN files.contracts.contract_number IS 'Số hợp đồng (unique)';
COMMENT ON COLUMN files.contracts.contract_type IS 'Loại hợp đồng: RENTAL, PURCHASE, etc.';
COMMENT ON COLUMN files.contracts.status IS 'Trạng thái: ACTIVE, EXPIRED, TERMINATED, etc.';

COMMENT ON TABLE files.contract_files IS 'Metadata của các file hợp đồng đã upload (PDF, images)';
COMMENT ON COLUMN files.contract_files.is_primary IS 'File chính của hợp đồng (sẽ hiển thị đầu tiên)';
COMMENT ON COLUMN files.contract_files.display_order IS 'Thứ tự hiển thị các files';
COMMENT ON COLUMN files.contract_files.file_path IS 'Đường dẫn file trên local storage (relative)';

