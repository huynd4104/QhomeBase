
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'vehicle_registration_status') THEN
        CREATE TYPE data.vehicle_registration_status AS ENUM ('PENDING','APPROVED','REJECTED','CANCELED');
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS data.vehicle_registration_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    vehicle_id UUID REFERENCES data.vehicles(id) ON DELETE CASCADE,
    reason TEXT,
    status data.vehicle_registration_status NOT NULL DEFAULT 'PENDING',
    requested_by UUID NOT NULL,
    approved_by UUID,
    note TEXT,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    

    CONSTRAINT fk_vehicle_registration_tenant FOREIGN KEY (tenant_id) REFERENCES data.tenants(id),
    CONSTRAINT fk_vehicle_registration_vehicle FOREIGN KEY (vehicle_id) REFERENCES data.vehicles(id),

    CONSTRAINT chk_vehicle_registration_status CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELED')),
    CONSTRAINT chk_vehicle_registration_reason CHECK (reason IS NULL OR LENGTH(reason) <= 500),
    CONSTRAINT chk_vehicle_registration_note CHECK (note IS NULL OR LENGTH(note) <= 500)
);

CREATE INDEX IF NOT EXISTS idx_vehicle_registration_tenant_id ON data.vehicle_registration_requests(tenant_id);
CREATE INDEX IF NOT EXISTS idx_vehicle_registration_vehicle_id ON data.vehicle_registration_requests(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_vehicle_registration_status ON data.vehicle_registration_requests(status);
CREATE INDEX IF NOT EXISTS idx_vehicle_registration_requested_by ON data.vehicle_registration_requests(requested_by);
CREATE INDEX IF NOT EXISTS idx_vehicle_registration_approved_by ON data.vehicle_registration_requests(approved_by);
CREATE INDEX IF NOT EXISTS idx_vehicle_registration_requested_at ON data.vehicle_registration_requests(requested_at);

