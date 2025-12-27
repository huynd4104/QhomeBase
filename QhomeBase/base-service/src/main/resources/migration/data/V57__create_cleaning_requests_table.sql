CREATE TABLE data.cleaning_requests (
    id UUID PRIMARY KEY,
    unit_id UUID NOT NULL REFERENCES data.units(id),
    resident_id UUID,
    created_by UUID NOT NULL,
    cleaning_type TEXT NOT NULL,
    cleaning_date DATE NOT NULL,
    start_time TIME NOT NULL,
    duration_hours NUMERIC(5,2) NOT NULL CHECK (duration_hours > 0),
    location TEXT NOT NULL,
    note TEXT,
    contact_phone TEXT NOT NULL,
    extra_services TEXT,
    payment_method TEXT,
    status TEXT NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cleaning_requests_unit ON data.cleaning_requests(unit_id);

