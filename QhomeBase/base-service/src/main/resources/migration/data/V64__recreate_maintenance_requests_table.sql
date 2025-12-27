CREATE TABLE IF NOT EXISTS data.maintenance_requests (
    id UUID PRIMARY KEY,
    unit_id UUID NOT NULL REFERENCES data.units(id),
    resident_id UUID,
    created_by UUID NOT NULL,
    maintenance_type TEXT NOT NULL,
    issue_description TEXT NOT NULL,
    requested_date DATE NOT NULL,
    preferred_time TIME,
    status TEXT NOT NULL DEFAULT 'PENDING',
    priority TEXT,
    contact_phone TEXT NOT NULL,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_maintenance_requests_unit ON data.maintenance_requests(unit_id);


