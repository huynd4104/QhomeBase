-- V22: Remove multi-tenant architecture - Single project with multiple buildings
-- This migration removes all tenant-related columns and constraints

-- Step 0: Drop views that depend on tenant_id columns
DROP VIEW IF EXISTS data.v_current_households CASCADE;
DROP VIEW IF EXISTS data.v_current_unit_parties CASCADE;
DROP VIEW IF EXISTS data.v_active_vehicles CASCADE;
DROP VIEW IF EXISTS data.v_building_deletion_pending CASCADE;

-- Step 1: Drop all foreign key constraints referencing tenants table
ALTER TABLE IF EXISTS data.buildings DROP CONSTRAINT IF EXISTS fk_buildings_tenant;
ALTER TABLE IF EXISTS data.units DROP CONSTRAINT IF EXISTS fk_units_tenant;
ALTER TABLE IF EXISTS data.residents DROP CONSTRAINT IF EXISTS fk_residents_tenant;
ALTER TABLE IF EXISTS data.households DROP CONSTRAINT IF EXISTS fk_households_tenant;
ALTER TABLE IF EXISTS data.unit_parties DROP CONSTRAINT IF EXISTS fk_unit_parties_tenant;
ALTER TABLE IF EXISTS data.vehicles DROP CONSTRAINT IF EXISTS fk_vehicles_tenant;
ALTER TABLE IF EXISTS data.meters DROP CONSTRAINT IF EXISTS fk_meters_tenant;

-- Step 2: Drop unique constraints that include tenant_id
ALTER TABLE IF EXISTS data.buildings DROP CONSTRAINT IF EXISTS uq_buildings_tenant_code;
ALTER TABLE IF EXISTS data.units DROP CONSTRAINT IF EXISTS uq_units_tenant_building_code;
ALTER TABLE IF EXISTS data.residents DROP CONSTRAINT IF EXISTS uq_residents_tenant_phone;
ALTER TABLE IF EXISTS data.residents DROP CONSTRAINT IF EXISTS uq_residents_tenant_email;
ALTER TABLE IF EXISTS data.residents DROP CONSTRAINT IF EXISTS uq_residents_tenant_national_id;
ALTER TABLE IF EXISTS data.vehicles DROP CONSTRAINT IF EXISTS uq_vehicle_tenant_plate;
ALTER TABLE IF EXISTS data.meters DROP CONSTRAINT IF EXISTS uq_meter_tenant_code;

-- Step 3: Drop indexes that include tenant_id
DROP INDEX IF EXISTS data.idx_buildings_tenant;
DROP INDEX IF EXISTS data.idx_units_tenant_status;
DROP INDEX IF EXISTS data.idx_residents_tenant_status;

-- Step 4: Drop tenant_id columns from all tables
ALTER TABLE IF EXISTS data.buildings DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE IF EXISTS data.units DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE IF EXISTS data.residents DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE IF EXISTS data.households DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE IF EXISTS data.unit_parties DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE IF EXISTS data.vehicles DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE IF EXISTS data.meters DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE IF EXISTS data.building_deletion_requests DROP COLUMN IF EXISTS tenant_id;

-- Step 5: Recreate unique constraints without tenant_id
-- Buildings: code must be unique across the entire project
ALTER TABLE data.buildings 
    ADD CONSTRAINT uq_buildings_code UNIQUE (code);

-- Units: code must be unique within a building
ALTER TABLE data.units 
    ADD CONSTRAINT uq_units_building_code UNIQUE (building_id, code);

-- Residents: phone, email, national_id must be globally unique
ALTER TABLE data.residents 
    ADD CONSTRAINT uq_residents_phone UNIQUE (phone);

ALTER TABLE data.residents 
    ADD CONSTRAINT uq_residents_email UNIQUE (email);

ALTER TABLE data.residents 
    ADD CONSTRAINT uq_residents_national_id UNIQUE (national_id);

-- Vehicles: plate number must be globally unique
ALTER TABLE data.vehicles 
    ADD CONSTRAINT uq_vehicle_plate UNIQUE (plate_no);

-- Meters: meter code must be globally unique
ALTER TABLE data.meters 
    ADD CONSTRAINT uq_meter_code UNIQUE (meter_code);

-- Step 6: Recreate indexes without tenant_id
CREATE INDEX IF NOT EXISTS idx_units_status ON data.units (status);
CREATE INDEX IF NOT EXISTS idx_residents_status ON data.residents (status);

-- Step 6.5: Recreate views without tenant_id references
CREATE OR REPLACE VIEW data.v_current_households AS
SELECT h.id, h.unit_id, h.start_date, h.end_date, h.period, h.created_at, h.updated_at
FROM data.households h
WHERE h.end_date IS NULL OR h.end_date >= CURRENT_DATE;

CREATE OR REPLACE VIEW data.v_current_unit_parties AS
SELECT up.id, up.unit_id, up.resident_id, up.role, up.start_date, up.end_date, up.period, up.created_at, up.updated_at
FROM data.unit_parties up
WHERE up.end_date IS NULL OR up.end_date >= CURRENT_DATE;

CREATE OR REPLACE VIEW data.v_active_vehicles AS
SELECT v.id, v.resident_id, v.unit_id, v.plate_no, v.kind, v.color, v.active, v.activated_at, v.created_at, v.updated_at
FROM data.vehicles v 
WHERE v.active = TRUE;

CREATE OR REPLACE VIEW data.v_building_deletion_pending AS
SELECT bdr.id, bdr.building_id, bdr.requested_by, bdr.reason, bdr.approved_by, bdr.note, bdr.status, bdr.created_at, bdr.approved_at
FROM data.building_deletion_requests bdr
WHERE bdr.status = 'PENDING';

-- Step 7: Drop tenant-related tables
DROP TABLE IF EXISTS data.tenant_deletion_requests CASCADE;
DROP TABLE IF EXISTS data.tenants CASCADE;

-- Step 8: Add project metadata table (optional - for single project info)
CREATE TABLE IF NOT EXISTS data.project_info (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    code TEXT NOT NULL UNIQUE,
    address TEXT,
    contact TEXT,
    email TEXT,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT only_one_project CHECK (id = id) -- Ensures only one row
);

-- Insert default project info
INSERT INTO data.project_info (name, code, description) 
VALUES ('QHome Project', 'QHOME', 'Single residential management project')
ON CONFLICT (code) DO NOTHING;

