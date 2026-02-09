CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE SCHEMA IF NOT EXISTS data;
CREATE SCHEMA IF NOT EXISTS iam;
CREATE SCHEMA IF NOT EXISTS svc;

CREATE TYPE data.asset_type AS ENUM (
    'AIR_CONDITIONER',
    'KITCHEN',
    'WATER_HEATER',
    'FURNITURE',
    'OTHER'
);
CREATE TYPE data.building_deletion_status AS ENUM (''PENDING'',''APPROVED'',''REJECTED'',''CANCELED'');
CREATE TYPE data.household_kind AS ENUM ('OWNER','TENANT','SERVICE');
CREATE TYPE data.inspection_status AS ENUM (
        'PENDING',
        'IN_PROGRESS',
        'COMPLETED',
        'CANCELLED'
    );
CREATE TYPE data.party_role AS ENUM ('OWNER','TENANT','RESIDENT','GUARD','CLEANER','MANAGER');
CREATE TYPE data.party_type AS ENUM ('OCCUPANCY','MANAGEMENT','MAINTENANCE');
CREATE TYPE data.resident_status AS ENUM ('ACTIVE','INACTIVE','BANNED');
CREATE TYPE data.service_type AS ENUM ('UTILITY', 'AMENITY', 'MAINTENANCE', 'OTHER');
CREATE TYPE data.service_unit AS ENUM ('KWH', 'M3', 'UNIT', 'MONTH', 'OTHER');
CREATE TYPE data.tenant_deletion_status AS ENUM (''PENDING'',''APPROVED'',''REJECTED'',''CANCELED'');
CREATE TYPE data.unit_status AS ENUM ('ACTIVE','VACANT','MAINTENANCE','INACTIVE');
CREATE TYPE data.vehicle_kind AS ENUM ('CAR','MOTORBIKE','BICYCLE','ELECTRIC_SCOOTER','OTHER');
CREATE TYPE data.vehicle_registration_status AS ENUM ('PENDING','APPROVED','REJECTED','CANCELED');
CREATE TYPE svc.card_event_type AS ENUM (
      'ISSUED','REVOKED','LOCKED','UNLOCKED','EXTENDED',
      'PACKAGE_ADDED','PACKAGE_REMOVED','ACCESS','OTHER'
    );
CREATE TYPE svc.card_status AS ENUM ('ACTIVE','LOCKED','REVOKED','EXPIRED');
CREATE TYPE svc.card_type AS ENUM ('ELEVATOR','PARKING','ACCESS');
CREATE TYPE svc.formula_type AS ENUM ('FLAT','TIER','EXPR');
CREATE TYPE svc.owner_type AS ENUM ('RESIDENT','STAFF','GUEST','OTHER');

CREATE TABLE IF NOT EXISTS data.Buildings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    address TEXT,
    code TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by TEXT NOT NULL DEFAULT 'system',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    name TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    tenant_id UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by TEXT,
    CONSTRAINT uq_buildings_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT fk_buildings_tenant FOREIGN KEY (tenant_id) REFERENCES data.tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_buildings_tenant ON data.Buildings (tenant_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_buildings_tenant_code_active_ci
    ON data.Buildings (tenant_id, lower(code))
    WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS data.account_creation_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    approved_at TIMESTAMP WITH TIME ZONE,
    approved_by UUID,
    auto_generate BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    email VARCHAR(255),
    password TEXT,
    proof_of_relation_image_url TEXT,
    rejected_at TIMESTAMP WITH TIME ZONE,
    rejected_by UUID,
    rejection_reason TEXT,
    requested_by UUID NOT NULL,
    resident_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    username VARCHAR(50),
    CONSTRAINT fk_account_creation_requests_resident 
        FOREIGN KEY (resident_id) REFERENCES data.residents(id) ON DELETE CASCADE,
    CONSTRAINT fk_account_creation_requests_requested_by 
        FOREIGN KEY (requested_by) REFERENCES iam.users(id) ON DELETE CASCADE,
    CONSTRAINT fk_account_creation_requests_approved_by 
        FOREIGN KEY (approved_by) REFERENCES iam.users(id) ON DELETE SET NULL,
    CONSTRAINT fk_account_creation_requests_rejected_by 
        FOREIGN KEY (rejected_by) REFERENCES iam.users(id) ON DELETE SET NULL,
    CONSTRAINT chk_account_creation_requests_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'))
);


CREATE TABLE IF NOT EXISTS data.asset_inspection_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id UUID NOT NULL REFERENCES data.assets(id) ON DELETE CASCADE,
    condition_status TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    damage_cost NUMERIC(15, 2) DEFAULT 0,
    inspection_id UUID NOT NULL REFERENCES data.asset_inspections(id) ON DELETE CASCADE,
    notes TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    checked             BOOLEAN NOT NULL DEFAULT FALSE,
    checked_at          TIMESTAMPTZ,
    checked_by          UUID
);

CREATE INDEX IF NOT EXISTS idx_asset_inspection_items_asset ON data.asset_inspection_items (asset_id);

CREATE TABLE IF NOT EXISTS data.asset_inspections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    completed_at TIMESTAMPTZ,
    completed_by UUID,
    contract_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    inspection_date DATE NOT NULL,
    inspector_id UUID,
    inspector_name TEXT,
    inspector_notes TEXT,
    invoice_id UUID,
    scheduled_date DATE,
    status data.inspection_status NOT NULL DEFAULT 'PENDING',
    total_damage_cost NUMERIC(15, 2) DEFAULT 0,
    unit_id UUID NOT NULL REFERENCES data.units(id) ON DELETE CASCADE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_asset_inspection_contract UNIQUE (contract_id)
);

CREATE INDEX IF NOT EXISTS idx_asset_inspections_contract ON data.asset_inspections (contract_id);
CREATE INDEX IF NOT EXISTS idx_asset_inspections_unit ON data.asset_inspections (unit_id);
CREATE INDEX IF NOT EXISTS idx_asset_inspections_status ON data.asset_inspections (status);
CREATE INDEX IF NOT EXISTS idx_asset_inspections_inspector ON data.asset_inspections (inspector_id);
CREATE INDEX IF NOT EXISTS idx_asset_inspections_invoice ON data.asset_inspections (invoice_id);
CREATE INDEX IF NOT EXISTS idx_asset_inspections_scheduled_date ON data.asset_inspections (scheduled_date);

CREATE TABLE IF NOT EXISTS data.assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    asset_code TEXT NOT NULL,
    asset_type data.asset_type,
    brand TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    description TEXT,
    installed_at DATE,
    model TEXT,
    name TEXT,
    purchase_date DATE,
    purchase_price NUMERIC(14, 2),
    removed_at DATE,
    serial_number TEXT,
    unit_id UUID NOT NULL REFERENCES data.units(id) ON DELETE CASCADE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    warranty_until DATE,
    CONSTRAINT uq_asset_code UNIQUE (asset_code),
    CONSTRAINT ck_assets_period CHECK (removed_at IS NULL OR removed_at >= installed_at),
    CONSTRAINT ck_assets_warranty CHECK (warranty_until IS NULL OR warranty_until >= installed_at)
);

CREATE INDEX IF NOT EXISTS idx_assets_unit ON data.assets (unit_id);
CREATE INDEX IF NOT EXISTS idx_assets_type ON data.assets (asset_type);
CREATE INDEX IF NOT EXISTS idx_assets_active ON data.assets (active);
CREATE INDEX IF NOT EXISTS idx_assets_code ON data.assets (asset_code);

CREATE TABLE IF NOT EXISTS data.building_deletion_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    approved_at TIMESTAMPTZ NULL,
    approved_by UUID NULL,
    building_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    note TEXT,
    reason TEXT,
    requested_by UUID NOT NULL,
    status data.building_deletion_status NOT NULL DEFAULT 'PENDING',
    tenant_id UUID NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_bdr_building_status
    ON data.building_deletion_requests (building_id, status);
CREATE INDEX IF NOT EXISTS ix_bdr_tenant_status
    ON data.building_deletion_requests (tenant_id, status);
CREATE UNIQUE INDEX IF NOT EXISTS uq_bdr_building_pending
    ON data.building_deletion_requests (building_id)
    WHERE status = 'PENDING';

CREATE TABLE IF NOT EXISTS data.cleaning_requests (
    id UUID PRIMARY KEY,
    cleaning_date DATE NOT NULL,
    cleaning_type TEXT NOT NULL,
    contact_phone TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    duration_hours NUMERIC(5,2) NOT NULL CHECK (duration_hours > 0),
    extra_services TEXT,
    last_resent_at TIMESTAMPTZ,
    location TEXT NOT NULL,
    note TEXT,
    payment_method TEXT,
    resend_alert_sent BOOLEAN NOT NULL DEFAULT FALSE,
    resident_id UUID,
    start_time TIME NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING',
    unit_id UUID NOT NULL REFERENCES data.units(id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    user_id UUID
);


CREATE TABLE IF NOT EXISTS data.common_area_maintenance_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_response TEXT,
    area_type TEXT NOT NULL,
    attachments TEXT,
    building_id UUID REFERENCES data.buildings(id),
    contact_name TEXT NOT NULL,
    contact_phone TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    description TEXT NOT NULL,
    location TEXT NOT NULL,
    note TEXT,
    resident_id UUID,
    status TEXT NOT NULL DEFAULT 'PENDING',
    title TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    user_id UUID
);


CREATE TABLE IF NOT EXISTS data.household_member_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    approved_at TIMESTAMPTZ,
    approved_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    household_id UUID NOT NULL,
    note TEXT,
    proof_of_relation_image_url TEXT,
    rejected_at TIMESTAMPTZ,
    rejected_by UUID,
    rejection_reason TEXT,
    relation TEXT,
    requested_by UUID NOT NULL,
    resident_dob DATE,
    resident_email TEXT,
    resident_full_name TEXT NOT NULL,
    resident_id UUID NOT NULL,
    resident_national_id TEXT,
    resident_phone TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_hmr_household FOREIGN KEY (household_id) REFERENCES data.households(id) ON DELETE CASCADE,
    CONSTRAINT fk_hmr_resident FOREIGN KEY (resident_id) REFERENCES data.residents(id) ON DELETE CASCADE,
    CONSTRAINT fk_hmr_requested_by FOREIGN KEY (requested_by) REFERENCES iam.users(id) ON DELETE CASCADE,
    CONSTRAINT fk_hmr_approved_by FOREIGN KEY (approved_by) REFERENCES iam.users(id) ON DELETE SET NULL,
    CONSTRAINT fk_hmr_rejected_by FOREIGN KEY (rejected_by) REFERENCES iam.users(id) ON DELETE SET NULL,
    CONSTRAINT ck_hmr_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_hmr_household ON data.household_member_requests (household_id);
CREATE INDEX IF NOT EXISTS idx_hmr_resident ON data.household_member_requests (resident_id);
CREATE INDEX IF NOT EXISTS idx_hmr_requested_by ON data.household_member_requests (requested_by);
CREATE INDEX IF NOT EXISTS idx_hmr_status ON data.household_member_requests (status);
CREATE INDEX IF NOT EXISTS idx_hmr_national_id
    ON data.household_member_requests (resident_national_id)
    WHERE resident_national_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS data.household_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    household_id UUID NOT NULL REFERENCES data.households(id) ON DELETE CASCADE,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    joined_at DATE,
    left_at DATE,
    proof_of_relation_image_url TEXT,
    relation TEXT,
    resident_id UUID NOT NULL REFERENCES data.residents(id) ON DELETE RESTRICT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_member_unique UNIQUE (household_id, resident_id),
    CONSTRAINT ck_member_period CHECK (left_at IS NULL OR joined_at IS NULL OR left_at >= joined_at)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_household_one_primary ON data.household_members (household_id) WHERE is_primary = TRUE;
CREATE INDEX IF NOT EXISTS idx_household_members_resident ON data.household_members (resident_id);

CREATE TABLE IF NOT EXISTS data.households (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    end_date DATE,
    kind data.household_kind NOT NULL,
    period DATERANGE GENERATED ALWAYS AS (daterange(start_date, COALESCE(end_date, 'infinity'::date), '[]')) STORED,
    primary_resident_id UUID REFERENCES data.residents(id),
    start_date DATE NOT NULL,
    tenant_id UUID NOT NULL,
    unit_id UUID NOT NULL REFERENCES data.units(id) ON DELETE CASCADE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_households_period CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT ex_households_no_overlap EXCLUDE USING GIST (unit_id WITH =, kind WITH =, period WITH &&),
    CONSTRAINT fk_households_tenant FOREIGN KEY (tenant_id) REFERENCES data.tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_households_unit ON data.households (unit_id);
CREATE INDEX IF NOT EXISTS idx_households_period ON data.households USING GIST (period);
CREATE INDEX IF NOT EXISTS idx_households_contract_id
    ON data.households (contract_id);
CREATE INDEX IF NOT EXISTS idx_households_unit_end_date 
ON data.households (unit_id, end_date NULLS FIRST);
CREATE INDEX IF NOT EXISTS idx_households_start_date_desc 
ON data.households (start_date DESC);
CREATE INDEX IF NOT EXISTS idx_households_primary_resident 
ON data.households (primary_resident_id) 
WHERE primary_resident_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS data.maintenance_requests (
    id UUID PRIMARY KEY,
    admin_response TEXT,
    attachments TEXT,
    call_alert_sent BOOLEAN NOT NULL DEFAULT FALSE,
    category TEXT,
    contact_name TEXT,
    contact_phone TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    description TEXT,
    estimated_cost NUMERIC(15, 2),
    last_resent_at TIMESTAMPTZ,
    location TEXT,
    note TEXT,
    preferred_datetime TIMESTAMPTZ,
    resend_alert_sent BOOLEAN NOT NULL DEFAULT FALSE,
    resident_id UUID,
    responded_at TIMESTAMPTZ,
    responded_by UUID,
    response_status VARCHAR(50),
    status TEXT,
    title TEXT,
    unit_id UUID NOT NULL REFERENCES data.units(id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    user_id UUID
);


CREATE TABLE IF NOT EXISTS data.meter_reading_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    assigned_by UUID NOT NULL,
    assigned_to UUID NOT NULL,
    building_id UUID,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    cycle_id UUID NOT NULL,
    end_date DATE,
    floor INTEGER,
    note TEXT,
    reminder_last_sent_date DATE,
    service_id UUID NOT NULL,
    start_date DATE,
    status TEXT,
    unit_ids UUID[],
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_assignment_cycle
        FOREIGN KEY (cycle_id) REFERENCES data.reading_cycles(id) ON DELETE CASCADE,
    CONSTRAINT fk_assignment_building
        FOREIGN KEY (building_id) REFERENCES data.buildings(id) ON DELETE CASCADE,
    CONSTRAINT fk_assignment_service FOREIGN KEY (service_id) REFERENCES data.services(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_assignments_cycle ON data.meter_reading_assignments (cycle_id);
CREATE INDEX IF NOT EXISTS idx_assignments_assigned_to ON data.meter_reading_assignments (assigned_to, completed_at);
CREATE INDEX IF NOT EXISTS idx_assignments_building_service ON data.meter_reading_assignments (building_id, service_id);
CREATE INDEX IF NOT EXISTS idx_assignments_due_date ON data.meter_reading_assignments (due_date, completed_at);
CREATE INDEX IF NOT EXISTS idx_assignments_unit_ids 
    ON data.meter_reading_assignments USING GIN(unit_ids) 
    WHERE unit_ids IS NOT NULL;

CREATE TABLE IF NOT EXISTS data.meter_reading_reminders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    acknowledged_at TIMESTAMPTZ,
    assignment_id UUID NOT NULL REFERENCES data.meter_reading_assignments(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    due_date DATE NOT NULL,
    message TEXT NOT NULL,
    title TEXT NOT NULL,
    type VARCHAR(100),
    user_id UUID NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_meter_reading_reminders_user
    ON data.meter_reading_reminders (user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS data.meter_readings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id UUID,
    consumption NUMERIC(14,3) GENERATED ALWAYS AS (curr_index - prev_index) STORED,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    curr_index NUMERIC(14,3) NOT NULL,
    cycle_id UUID,
    dispute_reason TEXT,
    disputed BOOLEAN DEFAULT FALSE,
    disputed_at TIMESTAMPTZ,
    disputed_by UUID,
    meter_id UUID NOT NULL,
    note TEXT,
    photo_file_id UUID,
    prev_index NUMERIC(14,3) NOT NULL,
    read_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    reader_id UUID NOT NULL,
    reading_date DATE NOT NULL,
    unit_id UUID,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    verified BOOLEAN DEFAULT false,
    verified_at TIMESTAMPTZ,
    verified_by UUID,
    CONSTRAINT fk_reading_meter
        FOREIGN KEY (meter_id) REFERENCES data.meters(id) ON DELETE CASCADE,
    CONSTRAINT fk_reading_session
        FOREIGN KEY (session_id) REFERENCES data.meter_reading_sessions(id) ON DELETE SET NULL,
    CONSTRAINT ck_reading_nonneg 
        CHECK (curr_index >= 0 AND prev_index >= 0 AND curr_index >= prev_index),
    CONSTRAINT fk_meter_reading_assignment FOREIGN KEY (assignment_id) REFERENCES data.meter_reading_assignments(id) ON DELETE CASCADE,
    CONSTRAINT fk_meter_readings_cycle FOREIGN KEY (cycle_id) REFERENCES data.reading_cycles(id) ON DELETE RESTRICT,
    CONSTRAINT uq_meter_reading_meter_date_cycle UNIQUE (meter_id, reading_date, cycle_id)
);

CREATE INDEX IF NOT EXISTS idx_readings_meter ON data.meter_readings (meter_id, reading_date DESC);
CREATE INDEX IF NOT EXISTS idx_readings_reader ON data.meter_readings (reader_id);
CREATE INDEX IF NOT EXISTS idx_readings_verified ON data.meter_readings (verified, verified_at);
CREATE INDEX IF NOT EXISTS idx_readings_date ON data.meter_readings (reading_date DESC);

CREATE TABLE IF NOT EXISTS data.meters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    installed_at DATE,
    meter_code TEXT NOT NULL,
    period DATERANGE GENERATED ALWAYS AS (daterange(installed_at, COALESCE(removed_at, 'infinity'::date), '[]')) STORED,
    removed_at DATE,
    service_id UUID NOT NULL REFERENCES svc.services(id) ON DELETE RESTRICT,
    tenant_id UUID NOT NULL,
    unit_id UUID NOT NULL REFERENCES data.units(id) ON DELETE CASCADE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_meter_tenant_code UNIQUE (tenant_id, meter_code),
    CONSTRAINT ck_meters_period CHECK (removed_at IS NULL OR removed_at >= installed_at),
    CONSTRAINT ex_meters_no_overlap EXCLUDE USING GIST (unit_id WITH =, service_id WITH =, period WITH &&),
    CONSTRAINT fk_meters_tenant FOREIGN KEY (tenant_id) REFERENCES data.tenants(id) ON DELETE CASCADE,
    CONSTRAINT uq_meter_code UNIQUE (meter_code),
    CONSTRAINT fk_meters_service FOREIGN KEY (service_id) REFERENCES data.services(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_meters_unit ON data.meters (unit_id);
CREATE INDEX IF NOT EXISTS idx_meters_service ON data.meters (service_id);
CREATE INDEX IF NOT EXISTS idx_meters_period ON data.meters USING GIST (period);

CREATE TABLE IF NOT EXISTS data.project_info (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    address TEXT,
    code TEXT NOT NULL UNIQUE,
    contact TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    description TEXT,
    email TEXT,
    name TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT only_one_project CHECK (id = id)
);


CREATE TABLE IF NOT EXISTS data.reading_cycles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    description TEXT,
    name TEXT NOT NULL,
    period_from DATE NOT NULL,
    period_to DATE NOT NULL,
    service_id UUID,
    status TEXT NOT NULL DEFAULT 'OPEN',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_reading_cycle_period CHECK (period_from <= period_to),
    CONSTRAINT ck_reading_cycle_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'COMPLETED', 'CLOSED')),
    CONSTRAINT fk_reading_cycle_service FOREIGN KEY (service_id) REFERENCES data.services(id),
    CONSTRAINT uq_reading_cycle_name_service UNIQUE (name, service_id)
);

CREATE INDEX IF NOT EXISTS idx_reading_cycles_status ON data.reading_cycles (status, period_from DESC);
CREATE INDEX IF NOT EXISTS idx_reading_cycles_period ON data.reading_cycles (period_from, period_to);

CREATE TABLE IF NOT EXISTS data.residents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    dob DATE,
    email TEXT,
    full_name TEXT NOT NULL,
    national_id TEXT,
    phone TEXT,
    status data.resident_status NOT NULL DEFAULT 'ACTIVE',
    tenant_id UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    user_id UUID,
    CONSTRAINT uq_residents_tenant_phone UNIQUE (tenant_id, phone),
    CONSTRAINT uq_residents_tenant_email UNIQUE (tenant_id, email),
    CONSTRAINT uq_residents_tenant_national_id UNIQUE (tenant_id, national_id),
    CONSTRAINT fk_residents_tenant FOREIGN KEY (tenant_id) REFERENCES data.tenants(id) ON DELETE CASCADE,
    CONSTRAINT uq_residents_phone UNIQUE (phone),
    CONSTRAINT uq_residents_email UNIQUE (email),
    CONSTRAINT uq_residents_national_id UNIQUE (national_id)
);

CREATE INDEX IF NOT EXISTS idx_residents_tenant_status ON data.residents (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_residents_name_trgm ON data.residents USING GIN (full_name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_residents_status ON data.residents (status);

CREATE TABLE IF NOT EXISTS data.services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    billable BOOLEAN NOT NULL DEFAULT TRUE,
    code TEXT NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    description TEXT,
    display_order INTEGER DEFAULT 0,
    name TEXT NOT NULL,
    name_en TEXT,
    requires_meter BOOLEAN NOT NULL DEFAULT FALSE,
    type data.service_type NOT NULL DEFAULT 'UTILITY',
    unit data.service_unit NOT NULL,
    unit_label TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_services_code UNIQUE (code)
);


CREATE TABLE IF NOT EXISTS data.unit_parties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    end_date DATE,
    party_type data.party_type NOT NULL,
    period DATERANGE GENERATED ALWAYS AS (daterange(start_date, COALESCE(end_date, 'infinity'::date), '[]')) STORED,
    resident_id UUID REFERENCES data.residents(id),
    role data.party_role NOT NULL,
    start_date DATE NOT NULL,
    tenant_id UUID NOT NULL,
    unit_id UUID NOT NULL REFERENCES data.units(id) ON DELETE CASCADE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_unit_parties_period CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT ex_unit_parties_no_overlap EXCLUDE USING GIST (unit_id WITH =, role WITH =, period WITH &&),
    CONSTRAINT fk_unit_parties_tenant FOREIGN KEY (tenant_id) REFERENCES data.tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_unit_parties_unit_role ON data.unit_parties (unit_id, role);
CREATE INDEX IF NOT EXISTS idx_unit_parties_resident ON data.unit_parties (resident_id);
CREATE INDEX IF NOT EXISTS idx_unit_parties_period ON data.unit_parties USING GIST (period);

CREATE TABLE IF NOT EXISTS data.units (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    area_m2 NUMERIC(10,2) CHECK (area_m2 IS NULL OR area_m2 >= 0),
    bedrooms INTEGER CHECK (bedrooms IS NULL OR bedrooms >= 0),
    building_id UUID NOT NULL REFERENCES data.Buildings(id) ON DELETE CASCADE,
    code TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    floor INTEGER,
    status data.unit_status NOT NULL DEFAULT 'ACTIVE',
    tenant_id UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_units_tenant_building_code UNIQUE (tenant_id, building_id, code),
    CONSTRAINT fk_units_tenant FOREIGN KEY (tenant_id) REFERENCES data.tenants(id) ON DELETE CASCADE,
    CONSTRAINT uq_units_building_code UNIQUE (building_id, code)
);

CREATE INDEX IF NOT EXISTS idx_units_tenant_status ON data.units (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_units_building ON data.units (building_id);
CREATE INDEX IF NOT EXISTS idx_units_status ON data.units (status);

CREATE TABLE IF NOT EXISTS data.vehicle_registration_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    approved_at TIMESTAMPTZ,
    approved_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    note TEXT,
    reason TEXT,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    requested_by UUID NOT NULL,
    status data.vehicle_registration_status NOT NULL DEFAULT 'PENDING',
    tenant_id UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    vehicle_id UUID REFERENCES data.vehicles(id) ON DELETE CASCADE,
    CONSTRAINT fk_vehicle_registration_tenant FOREIGN KEY (tenant_id) REFERENCES data.tenants(id),
    CONSTRAINT fk_vehicle_registration_vehicle FOREIGN KEY (vehicle_id) REFERENCES data.vehicles(id),
    CONSTRAINT chk_vehicle_registration_status CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELED')),
    CONSTRAINT chk_vehicle_registration_reason CHECK (reason IS NULL OR LENGTH(reason) <= 500),
    CONSTRAINT chk_vehicle_registration_note CHECK (note IS NULL OR LENGTH(note) <= 500)
);


CREATE TABLE IF NOT EXISTS data.vehicles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    activated_at TIMESTAMPTZ,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    approved_by UUID,
    color TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    kind data.vehicle_kind,
    plate_no TEXT NOT NULL,
    registration_approved_at TIMESTAMPTZ,
    resident_id UUID REFERENCES data.residents(id),
    tenant_id UUID NOT NULL,
    unit_id UUID REFERENCES data.units(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_vehicle_tenant_plate UNIQUE (tenant_id, plate_no),
    CONSTRAINT fk_vehicles_tenant FOREIGN KEY (tenant_id) REFERENCES data.tenants(id) ON DELETE CASCADE,
    CONSTRAINT uq_vehicle_plate UNIQUE (plate_no)
);

CREATE INDEX IF NOT EXISTS idx_vehicles_resident ON data.vehicles (resident_id);
CREATE INDEX IF NOT EXISTS idx_vehicles_unit ON data.vehicles (unit_id);

CREATE TABLE IF NOT EXISTS iam.users (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE IF NOT EXISTS svc.card_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    card_id UUID NOT NULL REFERENCES svc.cards(id) ON DELETE CASCADE,
    package_id UUID NOT NULL REFERENCES svc.card_packages(id) ON DELETE RESTRICT,
    tenant_id UUID NOT NULL,
    CONSTRAINT uq_card_assign UNIQUE (tenant_id, card_id, package_id)
);

CREATE INDEX IF NOT EXISTS idx_card_assign_card_time ON svc.card_assignments (card_id, assigned_at DESC);

CREATE TABLE IF NOT EXISTS svc.card_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID,
    card_id UUID NOT NULL REFERENCES svc.cards(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    event_type svc.card_event_type NOT NULL,
    note TEXT,
    tenant_id UUID NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_card_events_card_time ON svc.card_events (card_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_card_events_tenant_type_time ON svc.card_events (tenant_id, event_type, created_at DESC);

CREATE TABLE IF NOT EXISTS svc.card_packages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    rights_json JSONB NOT NULL,
    tenant_id UUID NOT NULL,
    CONSTRAINT uq_card_packages_tenant_name UNIQUE (tenant_id, name)
);


CREATE TABLE IF NOT EXISTS svc.cards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    card_no TEXT NOT NULL,
    expired_at TIMESTAMPTZ,
    issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    owner_id UUID,
    owner_type svc.owner_type NOT NULL,
    status svc.card_status NOT NULL DEFAULT 'ACTIVE',
    tenant_id UUID NOT NULL,
    type svc.card_type NOT NULL,
    CONSTRAINT uq_cards_tenant_cardno UNIQUE (tenant_id, card_no)
);

CREATE INDEX IF NOT EXISTS idx_cards_tenant_status ON svc.cards (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_cards_owner ON svc.cards (tenant_id, owner_type, owner_id);

CREATE TABLE IF NOT EXISTS svc.pricing_formulas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    effective_daterange DATERANGE GENERATED ALWAYS AS (daterange(effective_from, COALESCE(effective_to, 'infinity'::date), '[]')) STORED,
    effective_from DATE NOT NULL,
    effective_to DATE,
    formula_json JSONB NOT NULL,
    formula_type svc.formula_type NOT NULL,
    service_id UUID NOT NULL REFERENCES svc.services(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_pricing_formulas_tenant_service ON svc.pricing_formulas (tenant_id, service_id);

CREATE TABLE IF NOT EXISTS svc.pricing_tiers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    max_value NUMERIC(14,4),
    min_value NUMERIC(14,4) NOT NULL,
    price_per_unit NUMERIC(14,4) NOT NULL CHECK (price_per_unit >= 0),
    pricing_formula_id UUID NOT NULL REFERENCES svc.pricing_formulas(id) ON DELETE CASCADE,
    CONSTRAINT ck_tier_range CHECK (max_value IS NULL OR min_value <= max_value)
);

CREATE INDEX IF NOT EXISTS idx_tiers_formula_min ON svc.pricing_tiers (pricing_formula_id, min_value);

CREATE TABLE IF NOT EXISTS svc.services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    category TEXT,
    code TEXT NOT NULL,
    name TEXT NOT NULL,
    tenant_id UUID NOT NULL,
    unit TEXT,
    CONSTRAINT uq_services_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX IF NOT EXISTS idx_services_tenant_active ON svc.services (tenant_id) WHERE active = TRUE;

CREATE OR REPLACE VIEW data.v_active_vehicles AS
SELECT v.id, v.resident_id, v.unit_id, v.plate_no, v.kind, v.color, v.active, v.activated_at, v.created_at, v.updated_at
FROM data.vehicles v 
WHERE v.active = TRUE;

CREATE OR REPLACE VIEW data.v_building_deletion_pending AS
SELECT bdr.id, bdr.building_id, bdr.requested_by, bdr.reason, bdr.approved_by, bdr.note, bdr.status, bdr.created_at, bdr.approved_at
FROM data.building_deletion_requests bdr
WHERE bdr.status = 'PENDING';

CREATE OR REPLACE VIEW data.v_current_households AS
SELECT h.id, h.unit_id, h.start_date, h.end_date, h.period, h.created_at, h.updated_at
FROM data.households h
WHERE h.end_date IS NULL OR h.end_date >= CURRENT_DATE;

CREATE OR REPLACE VIEW data.v_current_unit_parties AS
SELECT up.id, up.unit_id, up.resident_id, up.role, up.start_date, up.end_date, up.period, up.created_at, up.updated_at
FROM data.unit_parties up
WHERE up.end_date IS NULL OR up.end_date >= CURRENT_DATE;

CREATE OR REPLACE VIEW data.v_meter_latest_reading_status AS
SELECT 
    m.id AS meter_id,
    m.meter_code,
    m.unit_id,
    u.code AS unit_code,
    u.floor,
    u.building_id,
    b.code AS building_code,
    b.name AS building_name,
    m.service_id,
    s.code AS service_code,
    s.name AS service_name,
    m.active,
    m.installed_at,
    latest_reading.reading_id,
    latest_reading.reading_date AS last_reading_date,
    latest_reading.curr_index AS last_index,
    latest_reading.verified AS last_verified,
    latest_reading.reader_id AS last_reader_id,
    latest_reading.read_at AS last_read_at,
    latest_reading.assignment_id AS last_assignment_id,
    latest_reading.cycle_id AS last_cycle_id,
    latest_reading.cycle_name AS last_cycle_name,
    CASE 
        WHEN latest_reading.reading_id IS NOT NULL THEN 'READ'
        ELSE 'NEVER_READ'
    END AS reading_status
FROM data.meters m
INNER JOIN data.units u ON m.unit_id = u.id
INNER JOIN data.buildings b ON u.building_id = b.id
INNER JOIN svc.services s ON m.service_id = s.id
LEFT JOIN LATERAL (
    SELECT 
        mr.id AS reading_id,
        mr.reading_date,
        mr.curr_index,
        mr.verified,
        mr.reader_id,
        mr.read_at,
        mr.assignment_id,
        mra.cycle_id,
        rc.name AS cycle_name
    FROM data.meter_readings mr
    LEFT JOIN data.meter_reading_assignments mra ON mr.assignment_id = mra.id
    LEFT JOIN data.reading_cycles rc ON mra.cycle_id = rc.id
    WHERE mr.meter_id = m.id
    ORDER BY mr.reading_date DESC, mr.read_at DESC
    LIMIT 1
) AS latest_reading ON TRUE
WHERE m.active = TRUE;

CREATE OR REPLACE VIEW data.v_meter_reading_status AS
SELECT 
    m.id AS meter_id,
    m.meter_code,
    m.unit_id,
    u.code AS unit_code,
    u.floor,
    u.building_id,
    b.code AS building_code,
    b.name AS building_name,
    m.service_id,
    s.code AS service_code,
    s.name AS service_name,
    mra.id AS assignment_id,
    mra.cycle_id,
    rc.name AS cycle_name,
    mra.status AS assignment_status,
    mra.start_date AS assignment_start_date,
    mra.end_date AS assignment_end_date,
    CASE 
        WHEN mr.id IS NOT NULL THEN 'READ'
        ELSE 'PENDING'
    END AS reading_status,
    mr.id AS reading_id,
    mr.reading_date,
    mr.prev_index,
    mr.curr_index,
    mr.consumption,
    mr.verified,
    mr.reader_id,
    mr.read_at,
    mr.verified_by,
    mr.verified_at,
    CASE 
        WHEN mr.id IS NOT NULL AND mr.verified = TRUE THEN 'VERIFIED'
        WHEN mr.id IS NOT NULL AND mr.verified = FALSE THEN 'READ'
        WHEN mr.id IS NULL AND mra.end_date < CURRENT_DATE THEN 'OVERDUE'
        WHEN mr.id IS NULL AND mra.start_date <= CURRENT_DATE AND mra.end_date >= CURRENT_DATE THEN 'PENDING'
        WHEN mr.id IS NULL THEN 'PENDING'
        ELSE 'UNKNOWN'
    END AS detailed_status
FROM data.meters m
INNER JOIN data.units u ON m.unit_id = u.id
INNER JOIN data.buildings b ON u.building_id = b.id
INNER JOIN svc.services s ON m.service_id = s.id
INNER JOIN data.meter_reading_assignments mra ON (
    mra.building_id = b.id 
    AND mra.service_id = s.id
    AND (
        (mra.unit_ids IS NULL AND mra.floor IS NULL) OR  
        (mra.unit_ids IS NULL AND mra.floor = u.floor) OR  
        (mra.unit_ids IS NOT NULL AND u.id = ANY(mra.unit_ids))  
    )
    AND mra.status NOT IN ('CANCELLED')
)
INNER JOIN data.reading_cycles rc ON mra.cycle_id = rc.id
LEFT JOIN data.meter_readings mr ON (
    mr.meter_id = m.id 
    AND mr.assignment_id = mra.id
)
WHERE m.active = TRUE;

CREATE OR REPLACE VIEW data.v_meter_reading_status_by_cycle AS
SELECT 
    rc.id AS cycle_id,
    rc.name AS cycle_name,
    rc.period_from,
    rc.period_to,
    rc.status AS cycle_status,
    m.service_id,
    s.code AS service_code,
    s.name AS service_name,
    b.id AS building_id,
    b.code AS building_code,
    b.name AS building_name,
    COUNT(DISTINCT m.id) AS total_meters,
    COUNT(DISTINCT CASE WHEN mr.id IS NOT NULL THEN m.id END) AS read_meters,
    COUNT(DISTINCT CASE WHEN mr.id IS NULL THEN m.id END) AS pending_meters,
    COUNT(DISTINCT CASE WHEN mr.id IS NOT NULL AND mr.verified = TRUE THEN m.id END) AS verified_meters,
    COUNT(DISTINCT CASE WHEN mr.id IS NULL AND mra.end_date < CURRENT_DATE THEN m.id END) AS overdue_meters,
    ROUND(
        CASE 
            WHEN COUNT(DISTINCT m.id) > 0 
            THEN (COUNT(DISTINCT CASE WHEN mr.id IS NOT NULL THEN m.id END)::NUMERIC / COUNT(DISTINCT m.id)::NUMERIC) * 100
            ELSE 0
        END, 2
    ) AS reading_progress_percentage
FROM data.reading_cycles rc
INNER JOIN data.meter_reading_assignments mra ON mra.cycle_id = rc.id
INNER JOIN data.buildings b ON mra.building_id = b.id
INNER JOIN svc.services s ON mra.service_id = s.id
INNER JOIN data.meters m ON (
    m.service_id = s.id
    AND m.unit_id IN (
        SELECT u.id FROM data.units u 
        WHERE u.building_id = b.id
        AND (
            (mra.unit_ids IS NULL AND mra.floor IS NULL) OR
            (mra.unit_ids IS NULL AND mra.floor = u.floor) OR
            (mra.unit_ids IS NOT NULL AND u.id = ANY(mra.unit_ids))
        )
    )
)
LEFT JOIN data.meter_readings mr ON (
    mr.meter_id = m.id 
    AND mr.assignment_id = mra.id
)
WHERE m.active = TRUE
  AND mra.status NOT IN ('CANCELLED')
GROUP BY 
    rc.id, rc.name, rc.period_from, rc.period_to, rc.status,
    m.service_id, s.code, s.name,
    b.id, b.code, b.name;

CREATE OR REPLACE VIEW data.v_meter_readings_detail AS
SELECT 
    mr.id,
    mr.meter_id,
    m.meter_code,
    mr.unit_id,
    u.code AS unit_code,
    u.building_id,
    b.code AS building_code,
    m.service_id,
    s.code AS service_code,
    s.name AS service_name,
    mr.reading_date,
    mr.prev_index,
    mr.curr_index,
    mr.consumption,
    mr.photo_file_id,
    mr.note,
    mr.reader_id,
    mr.read_at,
    mr.verified,
    mr.verified_by,
    mr.verified_at,
    mr.assignment_id
FROM data.meter_readings mr
JOIN data.meters m ON mr.meter_id = m.id
JOIN data.units u ON mr.unit_id = u.id
JOIN data.buildings b ON u.building_id = b.id
JOIN svc.services s ON m.service_id = s.id;

CREATE OR REPLACE VIEW data.v_meters_pending_reading AS
SELECT 
    m.id AS meter_id,
    m.meter_code,
    m.unit_id,
    u.code AS unit_code,
    u.building_id,
    b.code AS building_code,
    m.service_id,
    s.code AS service_code,
    s.name AS service_name,
    m.active,
    m.installed_at,
    (
        SELECT mr.reading_date
        FROM data.meter_readings mr
        WHERE mr.meter_id = m.id
        ORDER BY mr.reading_date DESC
        LIMIT 1
    ) AS last_reading_date,
    (
        SELECT mr.curr_index
        FROM data.meter_readings mr
        WHERE mr.meter_id = m.id
        ORDER BY mr.reading_date DESC
        LIMIT 1
    ) AS last_index
FROM data.meters m
JOIN data.units u ON m.unit_id = u.id
JOIN data.buildings b ON u.building_id = b.id
JOIN svc.services s ON m.service_id = s.id
WHERE m.active = true
  AND m.removed_at IS NULL;

CREATE OR REPLACE VIEW data.v_meters_with_reading_status AS
SELECT 
    m.id AS meter_id,
    m.meter_code,
    m.unit_id,
    u.code AS unit_code,
    u.floor,
    u.building_id,
    b.code AS building_code,
    b.name AS building_name,
    m.service_id,
    s.code AS service_code,
    s.name AS service_name,
    m.active,
    m.installed_at,
    mra.id AS assignment_id,
    mra.cycle_id,
    rc.name AS cycle_name,
    mra.start_date AS assignment_start_date,
    mra.end_date AS assignment_end_date,
    mr.id AS reading_id,
    mr.reading_date,
    mr.curr_index,
    mr.verified,
    CASE 
        WHEN mr.id IS NOT NULL THEN 'READ'
        ELSE 'PENDING'
    END AS reading_status,
    mr.reader_id,
    mr.read_at,
    mr.verified_by,
    mr.verified_at
FROM data.meters m
INNER JOIN data.units u ON m.unit_id = u.id
INNER JOIN data.buildings b ON u.building_id = b.id
INNER JOIN svc.services s ON m.service_id = s.id
INNER JOIN data.meter_reading_assignments mra ON (
    mra.building_id = b.id 
    AND mra.service_id = s.id
    AND (
        (mra.unit_ids IS NULL AND mra.floor IS NULL) OR  
        (mra.unit_ids IS NULL AND mra.floor = u.floor) OR  
        (mra.unit_ids IS NOT NULL AND u.id = ANY(mra.unit_ids))  
    )
)
INNER JOIN data.reading_cycles rc ON mra.cycle_id = rc.id
LEFT JOIN data.meter_readings mr ON (
    mr.meter_id = m.id 
    AND mr.assignment_id = mra.id
)
WHERE m.active = TRUE;

CREATE OR REPLACE VIEW data.v_reading_assignments_status AS
SELECT 
    a.id AS assignment_id,
    a.cycle_id,
    rc.name AS cycle_name,
    a.building_id,
    b.code AS building_code,
    b.name AS building_name,
    a.service_id,
    a.assigned_to,
    a.assigned_by,
    a.assigned_at,
    a.start_date,
    a.end_date,
    a.completed_at,
    a.floor,
    a.status,
    CASE 
        WHEN a.completed_at IS NOT NULL THEN 100
        WHEN a.end_date IS NULL OR a.start_date IS NULL THEN 0
        WHEN a.end_date < CURRENT_DATE THEN 0
        ELSE LEAST(100, GREATEST(0, 
            ROUND(
                ((CURRENT_DATE - a.start_date)::NUMERIC / 
                 NULLIF((a.end_date - a.start_date)::NUMERIC, 0)) * 100
            )::INTEGER
        ))
    END AS progress_percentage
FROM data.meter_reading_assignments a
INNER JOIN data.reading_cycles rc ON a.cycle_id = rc.id
LEFT JOIN data.buildings b ON a.building_id = b.id;

CREATE OR REPLACE VIEW data.v_reading_cycles_progress AS
SELECT 
    rc.id,
    rc.name,
    rc.period_from,
    rc.period_to,
    rc.status,
    rc.description,
    COUNT(DISTINCT mra.id) AS assignments_count,
    COUNT(DISTINCT CASE WHEN mra.completed_at IS NOT NULL THEN mra.id END) AS assignments_completed,
    COUNT(DISTINCT mr.id) AS readings_count,
    COUNT(DISTINCT CASE WHEN mr.verified THEN mr.id END) AS readings_verified,
    rc.created_by,
    rc.created_at
FROM data.reading_cycles rc
LEFT JOIN data.meter_reading_assignments mra ON mra.cycle_id = rc.id
LEFT JOIN data.meter_readings mr ON mr.assignment_id = mra.id
GROUP BY rc.id, rc.name, rc.period_from, rc.period_to, rc.status, 
         rc.description, rc.created_by, rc.created_at;

CREATE OR REPLACE VIEW data.v_reading_sessions_progress AS
SELECT
    s.id                                          AS session_id,
    s.cycle_id,
    s.building_id,
    s.service_id,
    s.reader_id,
    s.started_at,
    s.completed_at,
    CASE WHEN s.completed_at IS NULL THEN 'IN_PROGRESS' ELSE 'COMPLETED' END AS status,
    s.units_read
FROM data.meter_reading_sessions s;
