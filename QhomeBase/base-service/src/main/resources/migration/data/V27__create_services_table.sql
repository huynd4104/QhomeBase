CREATE TYPE data.service_type AS ENUM ('UTILITY', 'AMENITY', 'MAINTENANCE', 'OTHER');

CREATE TYPE data.service_unit AS ENUM ('KWH', 'M3', 'UNIT', 'MONTH', 'OTHER');

CREATE TABLE data.services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    name_en TEXT,
    type data.service_type NOT NULL DEFAULT 'UTILITY',
    unit data.service_unit NOT NULL,
    unit_label TEXT,
    billable BOOLEAN NOT NULL DEFAULT TRUE,
    requires_meter BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_services_code UNIQUE (code)
);

CREATE INDEX idx_services_type ON data.services(type);
CREATE INDEX idx_services_active ON data.services(active);

INSERT INTO data.services (code, name, name_en, type, unit, unit_label, billable, requires_meter, display_order) VALUES
('ELECTRIC', 'Điện', 'Electricity', 'UTILITY', 'KWH', 'kWh', TRUE, TRUE, 1),
('WATER', 'Nước', 'Water', 'UTILITY', 'M3', 'm³', TRUE, TRUE, 2),
('MANAGEMENT', 'Phí quản lý', 'Management Fee', 'UTILITY', 'MONTH', 'tháng', TRUE, FALSE, 3),
('PARKING', 'Phí gửi xe', 'Parking Fee', 'AMENITY', 'UNIT', 'xe', TRUE, FALSE, 4),
('INTERNET', 'Internet', 'Internet', 'AMENITY', 'MONTH', 'tháng', TRUE, FALSE, 5),
('CABLE_TV', 'Truyền hình cáp', 'Cable TV', 'AMENITY', 'MONTH', 'tháng', TRUE, FALSE, 6);

ALTER TABLE data.meters
ADD CONSTRAINT fk_meters_service FOREIGN KEY (service_id) REFERENCES data.services(id);

ALTER TABLE data.meter_reading_assignments
ADD CONSTRAINT fk_assignments_service FOREIGN KEY (service_id) REFERENCES data.services(id);

ALTER TABLE data.meter_reading_sessions
ADD CONSTRAINT fk_sessions_service FOREIGN KEY (service_id) REFERENCES data.services(id);

COMMENT ON TABLE data.services IS 'Danh mục dịch vụ (điện, nước, phí quản lý, v.v.)';
COMMENT ON COLUMN data.services.code IS 'Mã dịch vụ duy nhất (ELECTRIC, WATER, etc.)';
COMMENT ON COLUMN data.services.requires_meter IS 'TRUE nếu dịch vụ cần đồng hồ đo (điện, nước), FALSE nếu tính theo định mức (phí QL)';

