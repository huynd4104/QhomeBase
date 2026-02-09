/*
 * V1__Initial_Schema.sql
 *
 * Initial database schema for Asset Maintenance Service.
 * This file consolidates previous migrations to provide a clean starting state.
 *
 * Features:
 * - Assets Management (Elevators, AC, etc.)
 * - Maintenance Schedules & Records
 * - Service Bookings (Amenities)
 * - Suppliers & Contracts
 *
 */

CREATE SCHEMA IF NOT EXISTS asset;

-- =================================================================================================
-- 1. Enums and Configurations
-- =================================================================================================

-- Create types if they don't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'asset_type' AND typnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'asset')) THEN
        CREATE TYPE asset.asset_type AS ENUM (
            'ELEVATOR',
            'GENERATOR',
            'AIR_CONDITIONER',
            'WATER_PUMP',
            'SECURITY_SYSTEM',
            'FIRE_SAFETY',
            'CCTV',
            'INTERCOM',
            'GATE_BARRIER',
            'SWIMMING_POOL',
            'GYM_EQUIPMENT',
            'PLAYGROUND',
            'PARKING_SYSTEM',
            'GARDEN_IRRIGATION',
            'LIGHTING_SYSTEM',
            'WIFI_SYSTEM',
            'SOLAR_PANEL',
            'WASTE_MANAGEMENT',
            'MAINTENANCE_TOOL',
            'OTHER'
        );
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'asset_status' AND typnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'asset')) THEN
        CREATE TYPE asset.asset_status AS ENUM (
            'ACTIVE',
            'INACTIVE',
            'MAINTENANCE',
            'REPAIRING',
            'REPLACED',
            'DECOMMISSIONED'
        );
    END IF;
END$$;


-- =================================================================================================
-- 2. Master Data Tables
-- =================================================================================================

-- Table: asset.assets
-- Description: Stores information about building assets and infrastructure equipment.
CREATE TABLE IF NOT EXISTS asset.assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    building_id UUID NOT NULL, -- references data.buildings(id)
    unit_id UUID, -- references data.units(id), NULL for building-level assets
    
    code TEXT NOT NULL,
    name TEXT NOT NULL,
    asset_type asset.asset_type NOT NULL,
    status asset.asset_status NOT NULL DEFAULT 'ACTIVE',
    
    location TEXT,
    manufacturer TEXT,
    model TEXT,
    serial_number TEXT,
    tag_number TEXT,
    
    purchase_date DATE,
    purchase_price NUMERIC(15,2),
    current_value NUMERIC(15,2),
    replacement_cost NUMERIC(15,2),
    
    installation_date DATE,
    warranty_expiry_date DATE,
    decommission_date DATE,
    expected_lifespan_years INTEGER,
    
    description TEXT,
    notes TEXT,
    specifications JSONB,
    image_urls JSONB,
    
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by TEXT NOT NULL DEFAULT 'system',
    updated_by TEXT,

    CONSTRAINT ck_assets_warranty_date CHECK (warranty_expiry_date IS NULL OR purchase_date IS NULL OR warranty_expiry_date >= purchase_date),
    CONSTRAINT ck_assets_installation_date CHECK (installation_date IS NULL OR purchase_date IS NULL OR installation_date >= purchase_date),
    CONSTRAINT ck_assets_purchase_price CHECK (purchase_price IS NULL OR purchase_price >= 0),
    CONSTRAINT ck_assets_current_value CHECK (current_value IS NULL OR current_value >= 0),
    CONSTRAINT ck_assets_replacement_cost CHECK (replacement_cost IS NULL OR replacement_cost >= 0),
    CONSTRAINT ck_assets_lifespan CHECK (expected_lifespan_years IS NULL OR expected_lifespan_years > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_assets_building_code ON asset.assets(building_id, code) WHERE unit_id IS NULL AND is_deleted = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uq_assets_unit_code ON asset.assets(unit_id, code) WHERE unit_id IS NOT NULL AND is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_assets_building_id ON asset.assets(building_id);
CREATE INDEX IF NOT EXISTS idx_assets_unit_id ON asset.assets(unit_id) WHERE unit_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_assets_status ON asset.assets(status);
CREATE INDEX IF NOT EXISTS idx_assets_asset_type ON asset.assets(asset_type);


-- Table: asset.suppliers
-- Description: Stores supplier, service provider, and warranty provider information.
CREATE TABLE IF NOT EXISTS asset.suppliers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    type TEXT NOT NULL, -- 'SUPPLIER', 'SERVICE_PROVIDER', 'WARRANTY_PROVIDER', 'BOTH'
    contact_person TEXT,
    phone TEXT,
    email TEXT,
    address TEXT,
    tax_id TEXT,
    website TEXT,
    notes TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by TEXT NOT NULL DEFAULT 'system',
    updated_by TEXT
);

CREATE INDEX IF NOT EXISTS idx_suppliers_type ON asset.suppliers(type);
CREATE INDEX IF NOT EXISTS idx_suppliers_active ON asset.suppliers(is_active) WHERE is_active = TRUE;


-- Table: asset.asset_suppliers
-- Description: Junction table linking assets to suppliers.
CREATE TABLE IF NOT EXISTS asset.asset_suppliers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id UUID NOT NULL REFERENCES asset.assets(id) ON DELETE CASCADE,
    supplier_id UUID NOT NULL REFERENCES asset.suppliers(id) ON DELETE CASCADE,
    relationship_type TEXT NOT NULL DEFAULT 'PURCHASE', -- 'PURCHASE', 'WARRANTY', 'SERVICE'
    
    purchase_date DATE,
    purchase_price NUMERIC(15,2),
    
    warranty_start_date DATE,
    warranty_end_date DATE,
    warranty_provider TEXT,
    warranty_contact TEXT,
    
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by TEXT NOT NULL DEFAULT 'system',
    updated_by TEXT,
    
    CONSTRAINT ck_asset_suppliers_warranty_dates CHECK (warranty_end_date IS NULL OR warranty_start_date IS NULL OR warranty_end_date >= warranty_start_date),
    CONSTRAINT ck_asset_suppliers_price CHECK (purchase_price IS NULL OR purchase_price >= 0),
    UNIQUE(asset_id, supplier_id, relationship_type)
);

CREATE INDEX IF NOT EXISTS idx_asset_suppliers_asset_id ON asset.asset_suppliers(asset_id);
CREATE INDEX IF NOT EXISTS idx_asset_suppliers_supplier_id ON asset.asset_suppliers(supplier_id);


-- =================================================================================================
-- 3. Maintenance Tables
-- =================================================================================================

-- Table: asset.maintenance_schedules
-- Description: Stores recurring maintenance schedules/plans for assets.
CREATE TABLE IF NOT EXISTS asset.maintenance_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id UUID NOT NULL REFERENCES asset.assets(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    description TEXT,
    maintenance_type TEXT NOT NULL,
    interval_days INTEGER NOT NULL,
    start_date DATE NOT NULL,
    next_maintenance_date DATE NOT NULL,
    assigned_to UUID, -- references iam.users(id)
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by TEXT NOT NULL DEFAULT 'system',
    updated_by TEXT,
    
    CONSTRAINT ck_schedule_interval CHECK (interval_days > 0),
    CONSTRAINT ck_schedule_next_date CHECK (next_maintenance_date >= start_date),
    UNIQUE(asset_id, name)
);

CREATE INDEX IF NOT EXISTS idx_maintenance_schedules_asset_id ON asset.maintenance_schedules(asset_id);
CREATE INDEX IF NOT EXISTS idx_maintenance_schedules_next_maintenance ON asset.maintenance_schedules(next_maintenance_date) WHERE is_active = TRUE;


-- Table: asset.maintenance_records
-- Description: Stores maintenance history and scheduled maintenance for assets.
CREATE TABLE IF NOT EXISTS asset.maintenance_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id UUID NOT NULL REFERENCES asset.assets(id) ON DELETE CASCADE,
    maintenance_schedule_id UUID REFERENCES asset.maintenance_schedules(id) ON DELETE SET NULL,
    
    maintenance_type TEXT NOT NULL,
    maintenance_date DATE NOT NULL,
    description TEXT,
    status TEXT NOT NULL DEFAULT 'SCHEDULED', -- 'SCHEDULED', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'FAILED'
    
    assigned_to UUID NOT NULL, -- references iam.users(id)
    assigned_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    
    technician_report TEXT,
    completion_report TEXT,
    parts_replaced TEXT[],
    completion_images JSONB,
    
    cost NUMERIC(15,2),
    notes TEXT,
    
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by TEXT NOT NULL DEFAULT 'system',
    updated_by TEXT,

    CONSTRAINT ck_maintenance_cost CHECK (cost IS NULL OR cost >= 0),
    CONSTRAINT ck_maintenance_started CHECK (started_at IS NULL OR assigned_at IS NULL OR started_at >= assigned_at),
    CONSTRAINT ck_maintenance_completed CHECK (completed_at IS NULL OR started_at IS NULL OR completed_at >= started_at)
);

CREATE INDEX IF NOT EXISTS idx_maintenance_records_asset_id ON asset.maintenance_records(asset_id);
CREATE INDEX IF NOT EXISTS idx_maintenance_records_maintenance_date ON asset.maintenance_records(maintenance_date);
CREATE INDEX IF NOT EXISTS idx_maintenance_records_status ON asset.maintenance_records(status);


-- =================================================================================================
-- 4. Service & Amenities Tables
-- =================================================================================================

-- Table: asset.service_category
CREATE TABLE IF NOT EXISTS asset.service_category (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    icon VARCHAR(255),
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- Table: asset.service
CREATE TABLE IF NOT EXISTS asset.service (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL REFERENCES asset.service_category(id) ON DELETE RESTRICT,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    
    location VARCHAR(255),
    map_url TEXT,
    rules TEXT,
    
    pricing_type VARCHAR(50) NOT NULL DEFAULT 'HOURLY', -- 'HOURLY', 'SESSION', 'FREE'
    price_per_hour NUMERIC(15,2),
    price_per_session NUMERIC(15,2),
    
    min_duration_hours INTEGER NOT NULL DEFAULT 1,
    max_duration_hours INTEGER, -- CHECK (max_duration_hours >= min_duration_hours)
    max_capacity INTEGER,
    
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_service_pricing_type CHECK (pricing_type IN ('HOURLY', 'SESSION', 'FREE')),
    CONSTRAINT chk_service_duration CHECK (max_duration_hours IS NULL OR max_duration_hours >= min_duration_hours)
);


-- Table: asset.service_option
CREATE TABLE IF NOT EXISTS asset.service_option (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id UUID NOT NULL REFERENCES asset.service(id) ON DELETE CASCADE,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    price NUMERIC(15,2) NOT NULL,
    unit VARCHAR(100),
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- Table: asset.service_ticket
CREATE TABLE IF NOT EXISTS asset.service_ticket (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id UUID NOT NULL REFERENCES asset.service(id) ON DELETE CASCADE,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    ticket_type VARCHAR(50) NOT NULL, -- 'DAY', 'NIGHT', 'HOURLY', 'DAILY', 'FAMILY'
    price NUMERIC(15,2) NOT NULL,
    duration_hours NUMERIC(5,2),
    max_people INTEGER,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_service_ticket_type CHECK (ticket_type IN ('DAY', 'NIGHT', 'HOURLY', 'DAILY', 'FAMILY')),
    CONSTRAINT chk_service_ticket_duration CHECK (duration_hours IS NULL OR duration_hours > 0),
    CONSTRAINT chk_service_ticket_people CHECK (max_people IS NULL OR max_people > 0)
);


-- Table: asset.service_combo
CREATE TABLE IF NOT EXISTS asset.service_combo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id UUID NOT NULL REFERENCES asset.service(id) ON DELETE CASCADE,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    services_included TEXT,
    price NUMERIC(15,2) NOT NULL,
    duration_minutes INTEGER,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_service_combo_duration CHECK (duration_minutes IS NULL OR duration_minutes > 0)
);


-- Table: asset.service_combo_item
CREATE TABLE IF NOT EXISTS asset.service_combo_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    combo_id UUID NOT NULL REFERENCES asset.service_combo(id) ON DELETE CASCADE,
    item_name VARCHAR(255) NOT NULL,
    item_description TEXT,
    quantity INTEGER NOT NULL DEFAULT 1,
    item_price NUMERIC(15, 2) DEFAULT 0 NOT NULL,
    item_duration_minutes INTEGER,
    note TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_service_combo_item_quantity CHECK (quantity > 0)
);


-- Table: asset.service_availability
CREATE TABLE IF NOT EXISTS asset.service_availability (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id UUID NOT NULL REFERENCES asset.service(id) ON DELETE CASCADE,
    day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_service_availability_time CHECK (end_time > start_time)
);


-- Table: asset.service_booking
-- Description: Bookings for services/amenities.
CREATE TABLE IF NOT EXISTS asset.service_booking (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL, -- references iam.users(id)
    service_id UUID NOT NULL REFERENCES asset.service(id) ON DELETE RESTRICT,
    
    booking_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    duration_hours NUMERIC(5,2) NOT NULL,
    
    number_of_people INTEGER DEFAULT 1,
    purpose TEXT,
    terms_accepted BOOLEAN NOT NULL DEFAULT FALSE,
    
    total_amount NUMERIC(15,2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'APPROVED', 'REJECTED', 'COMPLETED', 'CANCELLED'
    payment_status VARCHAR(50) NOT NULL DEFAULT 'UNPAID', -- 'UNPAID', 'PAID', 'PENDING', 'CANCELLED'
    payment_gateway VARCHAR(100),
    payment_date TIMESTAMPTZ,
    vnpay_transaction_ref VARCHAR(100),
    
    approved_by UUID, -- references iam.users(id)
    approved_at TIMESTAMPTZ,
    rejection_reason TEXT,
    
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_service_booking_times CHECK (end_time > start_time),
    CONSTRAINT chk_service_booking_duration CHECK (duration_hours > 0),
    CONSTRAINT chk_service_booking_people CHECK (number_of_people IS NULL OR number_of_people > 0),
    CONSTRAINT chk_service_booking_payment_status CHECK (payment_status IN ('UNPAID', 'PAID', 'PENDING', 'CANCELLED')),
    CONSTRAINT chk_service_booking_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'COMPLETED', 'CANCELLED'))
);


-- Table: asset.service_booking_slot
CREATE TABLE IF NOT EXISTS asset.service_booking_slot (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id UUID NOT NULL REFERENCES asset.service(id) ON DELETE CASCADE,
    booking_id UUID NOT NULL REFERENCES asset.service_booking(id) ON DELETE CASCADE,
    slot_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_service_booking_slot_time CHECK (end_time > start_time)
);


-- Table: asset.service_booking_item
CREATE TABLE IF NOT EXISTS asset.service_booking_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES asset.service_booking(id) ON DELETE CASCADE,
    
    item_type VARCHAR(20) NOT NULL, -- 'OPTION', 'COMBO', 'TICKET'
    item_id UUID NOT NULL,
    item_code VARCHAR(100) NOT NULL,
    item_name VARCHAR(200) NOT NULL,
    metadata JSONB,
    
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price NUMERIC(15,2) NOT NULL,
    total_price NUMERIC(15,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_service_booking_item_type CHECK (item_type IN ('OPTION', 'COMBO', 'TICKET')),
    CONSTRAINT chk_service_booking_item_qty CHECK (quantity > 0),
    CONSTRAINT chk_service_booking_item_prices CHECK (unit_price >= 0 AND total_price >= 0)
);


-- =================================================================================================
-- 5. KPI Tables
-- =================================================================================================

-- Table: asset.service_kpi_metric
CREATE TABLE IF NOT EXISTS asset.service_kpi_metric (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id UUID REFERENCES asset.service(id) ON DELETE SET NULL,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    unit VARCHAR(50),
    calculation_method TEXT,
    frequency VARCHAR(20) NOT NULL DEFAULT 'MONTHLY', -- 'DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY'
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_service_kpi_frequency CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY')),
    CONSTRAINT uq_service_kpi_metric_code UNIQUE (service_id, code)
);


-- Table: asset.service_kpi_target
CREATE TABLE IF NOT EXISTS asset.service_kpi_target (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_id UUID NOT NULL REFERENCES asset.service_kpi_metric(id) ON DELETE CASCADE,
    service_id UUID REFERENCES asset.service(id) ON DELETE SET NULL,
    
    target_period_start DATE NOT NULL,
    target_period_end DATE NOT NULL,
    
    target_value NUMERIC(15,2),
    threshold_warning NUMERIC(15,2),
    threshold_critical NUMERIC(15,2),
    
    assigned_to UUID, -- references iam.users(id)
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_service_kpi_target_period CHECK (target_period_end >= target_period_start),
    CONSTRAINT uq_service_kpi_target UNIQUE (metric_id, target_period_start, target_period_end)
);


-- Table: asset.service_kpi_value
CREATE TABLE IF NOT EXISTS asset.service_kpi_value (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_id UUID NOT NULL REFERENCES asset.service_kpi_metric(id) ON DELETE CASCADE,
    service_id UUID REFERENCES asset.service(id) ON DELETE SET NULL,
    
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    
    actual_value NUMERIC(15,2),
    variance NUMERIC(15,2),
    status VARCHAR(20) NOT NULL DEFAULT 'FINAL', -- 'DRAFT', 'FINAL'
    source VARCHAR(20) NOT NULL DEFAULT 'SYSTEM', -- 'SYSTEM', 'MANUAL'
    
    recorded_by UUID, -- references iam.users(id)
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_service_kpi_value_period CHECK (period_end >= period_start),
    CONSTRAINT chk_service_kpi_value_status CHECK (status IN ('DRAFT', 'FINAL')),
    CONSTRAINT chk_service_kpi_value_source CHECK (source IN ('SYSTEM', 'MANUAL')),
    CONSTRAINT uq_service_kpi_value UNIQUE (metric_id, period_start, period_end)
);
