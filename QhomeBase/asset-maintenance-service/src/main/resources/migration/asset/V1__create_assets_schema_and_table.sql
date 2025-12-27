-- V1: Create assets schema and assets table
-- This migration creates the asset maintenance schema and assets table for managing building assets

-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS asset;

-- Create enum types for asset management
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

-- Create assets table
CREATE TABLE IF NOT EXISTS asset.assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    building_id UUID NOT NULL REFERENCES data.buildings(id) ON DELETE CASCADE,
    unit_id UUID REFERENCES data.units(id) ON DELETE SET NULL, -- NULL for building-level assets, set for unit-level assets
    code TEXT NOT NULL,
    name TEXT NOT NULL,
    asset_type asset.asset_type NOT NULL,
    status asset.asset_status NOT NULL DEFAULT 'ACTIVE',
    location TEXT, -- Location within building (e.g., "Lobby", "Basement", "Floor 5", "Parking Level 2", "Unit A-101")
    manufacturer TEXT,
    model TEXT,
    serial_number TEXT,
    purchase_date DATE,
    purchase_price NUMERIC(15,2), -- Purchase cost in VND or currency
    current_value NUMERIC(15,2), -- Current estimated value
    replacement_cost NUMERIC(15,2), -- Estimated replacement cost
    warranty_expiry_date DATE,
    installation_date DATE,
    expected_lifespan_years INTEGER, -- Expected lifespan in years
    decommission_date DATE, -- Date when asset was decommissioned
    tag_number TEXT, -- Physical tag number for asset identification
    image_urls JSONB, -- Array of image URLs for asset photos
    description TEXT,
    notes TEXT, -- Additional notes/comments
    specifications JSONB, -- Additional specifications as JSON (e.g., capacity, power consumption, etc.)
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
    -- Note: Unit must belong to the same building. This is enforced at application layer via service validation
);

-- Create unique indexes for code uniqueness (using partial indexes for building-level vs unit-level)
-- Code must be unique within a building for building-level assets (unit_id IS NULL)
CREATE UNIQUE INDEX IF NOT EXISTS uq_assets_building_code ON asset.assets(building_id, code) 
WHERE unit_id IS NULL AND is_deleted = FALSE;

-- Code must be unique within a unit for unit-level assets (unit_id IS NOT NULL)
CREATE UNIQUE INDEX IF NOT EXISTS uq_assets_unit_code ON asset.assets(unit_id, code) 
WHERE unit_id IS NOT NULL AND is_deleted = FALSE;

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_assets_building_id ON asset.assets(building_id);
CREATE INDEX IF NOT EXISTS idx_assets_unit_id ON asset.assets(unit_id) WHERE unit_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_assets_status ON asset.assets(status);
CREATE INDEX IF NOT EXISTS idx_assets_asset_type ON asset.assets(asset_type);
CREATE INDEX IF NOT EXISTS idx_assets_is_deleted ON asset.assets(is_deleted);
CREATE INDEX IF NOT EXISTS idx_assets_building_status ON asset.assets(building_id, status);
CREATE INDEX IF NOT EXISTS idx_assets_unit_status ON asset.assets(unit_id, status) WHERE unit_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_assets_warranty_expiry ON asset.assets(warranty_expiry_date) WHERE warranty_expiry_date IS NOT NULL;

-- Create partial index for active assets (most commonly queried)
CREATE INDEX IF NOT EXISTS idx_assets_active_building ON asset.assets(building_id, asset_type) 
WHERE is_deleted = FALSE AND status = 'ACTIVE' AND unit_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_assets_active_unit ON asset.assets(unit_id, asset_type) 
WHERE is_deleted = FALSE AND status = 'ACTIVE' AND unit_id IS NOT NULL;

-- Add comments for documentation
COMMENT ON SCHEMA asset IS 'Schema for asset maintenance service - manages building infrastructure assets';
COMMENT ON TABLE asset.assets IS 'Stores information about building assets and infrastructure equipment. Can be building-level (unit_id = NULL) or unit-level (unit_id != NULL)';
COMMENT ON COLUMN asset.assets.building_id IS 'Foreign key to data.buildings table';
COMMENT ON COLUMN asset.assets.unit_id IS 'Foreign key to data.units table. NULL for building-level assets (elevators, generators, etc.), set for unit-level assets (AC units, water heaters, etc.)';
COMMENT ON COLUMN asset.assets.code IS 'Unique asset code within a building (if unit_id is NULL) or within a unit (if unit_id is NOT NULL). Examples: "ELEVATOR-01", "GEN-01" for building-level, "AC-01", "WH-01" for unit-level';
COMMENT ON COLUMN asset.assets.asset_type IS 'Type of asset (elevator, generator, AC, etc.)';
COMMENT ON COLUMN asset.assets.location IS 'Physical location within the building or unit (e.g., "Lobby", "Basement", "Floor 5", "Unit A-101", "Kitchen")';
COMMENT ON COLUMN asset.assets.purchase_price IS 'Purchase cost of the asset';
COMMENT ON COLUMN asset.assets.current_value IS 'Current estimated value of the asset';
COMMENT ON COLUMN asset.assets.replacement_cost IS 'Estimated cost to replace the asset';
COMMENT ON COLUMN asset.assets.expected_lifespan_years IS 'Expected useful lifespan in years';
COMMENT ON COLUMN asset.assets.tag_number IS 'Physical tag/label number attached to asset for identification';
COMMENT ON COLUMN asset.assets.image_urls IS 'Array of image URLs for asset photos (stored as JSONB)';
COMMENT ON COLUMN asset.assets.specifications IS 'Additional specifications stored as JSONB (flexible schema)';

-- Create suppliers/vendors table for managing supplier and service provider information
CREATE TABLE IF NOT EXISTS asset.suppliers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    type TEXT NOT NULL, -- 'SUPPLIER', 'SERVICE_PROVIDER', 'WARRANTY_PROVIDER', 'BOTH'
    contact_person TEXT,
    phone TEXT,
    email TEXT,
    address TEXT,
    tax_id TEXT, -- Tax identification number
    website TEXT,
    notes TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by TEXT NOT NULL DEFAULT 'system',
    updated_by TEXT
);

-- Create maintenance schedules table for recurring maintenance plans per asset
CREATE TABLE IF NOT EXISTS asset.maintenance_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id UUID NOT NULL REFERENCES asset.assets(id) ON DELETE CASCADE,
    maintenance_type TEXT NOT NULL, -- 'ROUTINE', 'REPAIR', 'INSPECTION', 'EMERGENCY', 'UPGRADE'
    name TEXT NOT NULL, -- Schedule name/description (e.g., "Monthly Elevator Inspection", "Quarterly AC Cleaning")
    description TEXT, -- Detailed description of what needs to be done
    interval_days INTEGER NOT NULL, -- Maintenance interval in days (e.g., 30, 90, 180, 365)
    start_date DATE NOT NULL, -- When this schedule starts
    next_maintenance_date DATE NOT NULL, -- Next scheduled maintenance date (calculated)
    assigned_to UUID, -- Default TECHNICIAN staff/user ID to assign maintenance tasks (references iam.users.id, must have TECHNICIAN role)
    is_active BOOLEAN NOT NULL DEFAULT TRUE, -- Whether this schedule is active
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by TEXT NOT NULL DEFAULT 'system',
    updated_by TEXT,
    CONSTRAINT ck_schedule_interval CHECK (interval_days > 0),
    CONSTRAINT ck_schedule_next_date CHECK (next_maintenance_date >= start_date),
    UNIQUE(asset_id, name) -- One schedule name per asset
);

-- Create maintenance records table for tracking maintenance history
CREATE TABLE IF NOT EXISTS asset.maintenance_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id UUID NOT NULL REFERENCES asset.assets(id) ON DELETE CASCADE,
    maintenance_type TEXT NOT NULL, -- 'ROUTINE', 'REPAIR', 'INSPECTION', 'EMERGENCY', 'UPGRADE'
    maintenance_date DATE NOT NULL,
    maintenance_schedule_id UUID REFERENCES asset.maintenance_schedules(id) ON DELETE SET NULL, -- Reference to maintenance schedule if created from schedule
    assigned_to UUID NOT NULL, -- TECHNICIAN staff/user ID assigned to perform maintenance (references iam.users.id, must have TECHNICIAN role)
    assigned_at TIMESTAMPTZ, -- When the maintenance was assigned to TECHNICIAN staff
    started_at TIMESTAMPTZ, -- When TECHNICIAN staff started the maintenance
    description TEXT, -- Planned maintenance description / what needs to be done
    completion_report TEXT, -- Staff report after completing maintenance
    technician_report TEXT, -- Detailed technician report
    notes TEXT, -- Additional notes
    cost NUMERIC(15,2), -- Maintenance cost
    parts_replaced TEXT[], -- Array of parts replaced
    completion_images JSONB, -- Array of image URLs after maintenance completion
    status TEXT NOT NULL DEFAULT 'SCHEDULED', -- 'SCHEDULED', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'FAILED'
    completed_at TIMESTAMPTZ, -- When maintenance was completed
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by TEXT NOT NULL DEFAULT 'system',
    updated_by TEXT,
    CONSTRAINT ck_maintenance_cost CHECK (cost IS NULL OR cost >= 0),
    CONSTRAINT ck_maintenance_started CHECK (started_at IS NULL OR assigned_at IS NULL OR started_at >= assigned_at),
    CONSTRAINT ck_maintenance_completed CHECK (completed_at IS NULL OR started_at IS NULL OR completed_at >= started_at)
);

-- Create asset suppliers relationship table (for purchase suppliers)
CREATE TABLE IF NOT EXISTS asset.asset_suppliers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id UUID NOT NULL REFERENCES asset.assets(id) ON DELETE CASCADE,
    supplier_id UUID NOT NULL REFERENCES asset.suppliers(id) ON DELETE CASCADE,
    relationship_type TEXT NOT NULL DEFAULT 'PURCHASE', -- 'PURCHASE', 'WARRANTY', 'SERVICE'
    purchase_date DATE,
    purchase_price NUMERIC(15,2),
    warranty_start_date DATE,
    warranty_end_date DATE,
    warranty_provider TEXT, -- Denormalized warranty provider name
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

-- Create indexes for suppliers table
CREATE INDEX IF NOT EXISTS idx_suppliers_type ON asset.suppliers(type);
CREATE INDEX IF NOT EXISTS idx_suppliers_active ON asset.suppliers(is_active) WHERE is_active = TRUE;

-- Add foreign key constraints for assigned_to fields (must be added after table creation)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_maintenance_schedules_assigned_to'
    ) THEN
        ALTER TABLE asset.maintenance_schedules 
            ADD CONSTRAINT fk_maintenance_schedules_assigned_to 
            FOREIGN KEY (assigned_to) REFERENCES iam.users(id) ON DELETE SET NULL;
    END IF;
    
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_maintenance_records_assigned_to'
    ) THEN
        ALTER TABLE asset.maintenance_records 
            ADD CONSTRAINT fk_maintenance_records_assigned_to 
            FOREIGN KEY (assigned_to) REFERENCES iam.users(id) ON DELETE RESTRICT;
    END IF;
END$$;

-- Create indexes for maintenance_schedules table
CREATE INDEX IF NOT EXISTS idx_maintenance_schedules_asset_id ON asset.maintenance_schedules(asset_id);
CREATE INDEX IF NOT EXISTS idx_maintenance_schedules_next_maintenance ON asset.maintenance_schedules(next_maintenance_date) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_maintenance_schedules_active ON asset.maintenance_schedules(is_active) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_maintenance_schedules_assigned_to ON asset.maintenance_schedules(assigned_to) WHERE assigned_to IS NOT NULL;
-- Index for finding schedules with upcoming maintenance (using next_maintenance_date for query filtering)
CREATE INDEX IF NOT EXISTS idx_maintenance_schedules_next_date ON asset.maintenance_schedules(asset_id, next_maintenance_date) 
WHERE is_active = TRUE;
-- Index for finding schedules assigned to specific TECHNICIAN
CREATE INDEX IF NOT EXISTS idx_maintenance_schedules_assigned_technician ON asset.maintenance_schedules(assigned_to, next_maintenance_date) 
WHERE assigned_to IS NOT NULL AND is_active = TRUE;

-- Create indexes for maintenance_records table
CREATE INDEX IF NOT EXISTS idx_maintenance_records_asset_id ON asset.maintenance_records(asset_id);
CREATE INDEX IF NOT EXISTS idx_maintenance_records_maintenance_date ON asset.maintenance_records(maintenance_date);
CREATE INDEX IF NOT EXISTS idx_maintenance_records_schedule_id ON asset.maintenance_records(maintenance_schedule_id) WHERE maintenance_schedule_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_maintenance_records_status ON asset.maintenance_records(status);
CREATE INDEX IF NOT EXISTS idx_maintenance_records_assigned_to ON asset.maintenance_records(assigned_to);
-- Index for finding maintenance tasks assigned to TECHNICIAN staff
CREATE INDEX IF NOT EXISTS idx_maintenance_records_assigned_pending ON asset.maintenance_records(assigned_to, status) 
WHERE status IN ('ASSIGNED', 'IN_PROGRESS');

-- Create indexes for asset_suppliers table
CREATE INDEX IF NOT EXISTS idx_asset_suppliers_asset_id ON asset.asset_suppliers(asset_id);
CREATE INDEX IF NOT EXISTS idx_asset_suppliers_supplier_id ON asset.asset_suppliers(supplier_id);
CREATE INDEX IF NOT EXISTS idx_asset_suppliers_type ON asset.asset_suppliers(relationship_type);
CREATE INDEX IF NOT EXISTS idx_asset_suppliers_warranty_expiry ON asset.asset_suppliers(warranty_end_date) WHERE warranty_end_date IS NOT NULL;

-- Add comments for new tables
COMMENT ON TABLE asset.suppliers IS 'Stores supplier, service provider, and warranty provider information';
COMMENT ON COLUMN asset.suppliers.type IS 'Type of supplier: SUPPLIER, SERVICE_PROVIDER, WARRANTY_PROVIDER, or BOTH';
COMMENT ON TABLE asset.maintenance_schedules IS 'Stores recurring maintenance schedules/plans for assets. Allows importing and scheduling maintenance for multiple assets';
COMMENT ON COLUMN asset.maintenance_schedules.maintenance_type IS 'Type of maintenance: ROUTINE, REPAIR, INSPECTION, EMERGENCY, UPGRADE';
COMMENT ON COLUMN asset.maintenance_schedules.name IS 'Schedule name/description (e.g., "Monthly Elevator Inspection", "Quarterly AC Cleaning")';
COMMENT ON COLUMN asset.maintenance_schedules.interval_days IS 'Maintenance interval in days (e.g., 30 for monthly, 90 for quarterly, 180 for semi-annual, 365 for annual)';
COMMENT ON COLUMN asset.maintenance_schedules.next_maintenance_date IS 'Next scheduled maintenance date (calculated and updated after each maintenance)';
COMMENT ON COLUMN asset.maintenance_schedules.assigned_to IS 'Default TECHNICIAN staff/user ID to assign when creating maintenance records from this schedule (references iam.users.id, must have TECHNICIAN role)';
COMMENT ON COLUMN asset.maintenance_schedules.is_active IS 'Whether this schedule is active (can be deactivated without deleting)';
COMMENT ON TABLE asset.maintenance_records IS 'Stores maintenance history and scheduled maintenance for assets. Supports staff assignment and completion reporting';
COMMENT ON COLUMN asset.maintenance_records.maintenance_type IS 'Type of maintenance: ROUTINE, REPAIR, INSPECTION, EMERGENCY, UPGRADE';
COMMENT ON COLUMN asset.maintenance_records.maintenance_schedule_id IS 'Reference to maintenance_schedules if this record was created from a recurring schedule';
COMMENT ON COLUMN asset.maintenance_records.assigned_to IS 'TECHNICIAN staff/user ID assigned to perform this maintenance task (references iam.users.id, user must have TECHNICIAN role - only internal staff, no external service)';
COMMENT ON COLUMN asset.maintenance_records.assigned_at IS 'Timestamp when maintenance was assigned to TECHNICIAN staff';
COMMENT ON COLUMN asset.maintenance_records.started_at IS 'Timestamp when TECHNICIAN staff started working on the maintenance';
COMMENT ON COLUMN asset.maintenance_records.description IS 'Planned maintenance description / what needs to be done';
COMMENT ON COLUMN asset.maintenance_records.completion_report IS 'TECHNICIAN staff report after completing maintenance (what was done, issues found, etc.)';
COMMENT ON COLUMN asset.maintenance_records.technician_report IS 'Detailed technical report from TECHNICIAN staff';
COMMENT ON COLUMN asset.maintenance_records.completion_images IS 'Array of image URLs documenting maintenance completion (stored as JSONB)';
COMMENT ON COLUMN asset.maintenance_records.status IS 'Status: SCHEDULED, ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED, FAILED';
COMMENT ON COLUMN asset.maintenance_records.parts_replaced IS 'Array of parts/components that were replaced during maintenance';
COMMENT ON TABLE asset.asset_suppliers IS 'Junction table linking assets to suppliers (for purchase, warranty, service relationships)';

