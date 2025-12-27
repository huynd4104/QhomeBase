CREATE TABLE data.maintenance_requests (
    id UUID PRIMARY KEY,
    unit_id UUID NOT NULL REFERENCES data.units(id),
    resident_id UUID,
    created_by UUID NOT NULL,
    category TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    attachments TEXT,
    location TEXT NOT NULL,
    preferred_datetime TIMESTAMPTZ,
    contact_name TEXT NOT NULL,
    contact_phone TEXT NOT NULL,
    note TEXT,
    status TEXT NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_maintenance_requests_unit ON data.maintenance_requests(unit_id);

