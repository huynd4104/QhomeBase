CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE SCHEMA IF NOT EXISTS data;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'unit_status') THEN
CREATE TYPE data.unit_status AS ENUM ('ACTIVE','VACANT','MAINTENANCE','INACTIVE');
END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'resident_status') THEN
CREATE TYPE data.resident_status AS ENUM ('ACTIVE','INACTIVE','BANNED');
END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'household_kind') THEN
CREATE TYPE data.household_kind AS ENUM ('OWNER','TENANT','SERVICE');
END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'party_type') THEN
CREATE TYPE data.party_type AS ENUM ('OCCUPANCY','MANAGEMENT','MAINTENANCE');
END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'party_role') THEN
CREATE TYPE data.party_role AS ENUM ('OWNER','TENANT','RESIDENT','GUARD','CLEANER','MANAGER');
END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'vehicle_kind') THEN
CREATE TYPE data.vehicle_kind AS ENUM ('CAR','MOTORBIKE','BICYCLE','ELECTRIC_SCOOTER','OTHER');
END IF;
END$$;

CREATE TABLE IF NOT EXISTS data.Buildings (
                                              id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID NOT NULL,
    code       TEXT NOT NULL,
    name       TEXT NOT NULL,
    address    TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_buildings_tenant_code UNIQUE (tenant_id, code)
    );

CREATE TABLE IF NOT EXISTS data.units (
                                          id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL,
    building_id UUID NOT NULL REFERENCES data.Buildings(id) ON DELETE CASCADE,
    code        TEXT NOT NULL,
    floor       INTEGER,
    area_m2     NUMERIC(10,2) CHECK (area_m2 IS NULL OR area_m2 >= 0),
    bedrooms    INTEGER CHECK (bedrooms IS NULL OR bedrooms >= 0),
    status      data.unit_status NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_units_tenant_building_code UNIQUE (tenant_id, building_id, code)
    );

CREATE TABLE IF NOT EXISTS data.residents (
                                              id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID NOT NULL,
    full_name    TEXT NOT NULL,
    phone        TEXT,
    email        TEXT,
    national_id  TEXT,
    dob          DATE,
    status       data.resident_status NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_residents_tenant_phone UNIQUE (tenant_id, phone),
    CONSTRAINT uq_residents_tenant_email UNIQUE (tenant_id, email),
    CONSTRAINT uq_residents_tenant_national_id UNIQUE (tenant_id, national_id)
    );

CREATE TABLE IF NOT EXISTS data.households (
                                               id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id            UUID NOT NULL,
    unit_id              UUID NOT NULL REFERENCES data.units(id) ON DELETE CASCADE,
    kind                 data.household_kind NOT NULL,
    primary_resident_id  UUID REFERENCES data.residents(id),
    start_date           DATE NOT NULL,
    end_date             DATE,
    period               DATERANGE GENERATED ALWAYS AS (daterange(start_date, COALESCE(end_date, 'infinity'::date), '[]')) STORED,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_households_period CHECK (end_date IS NULL OR end_date >= start_date)
    );

ALTER TABLE data.households
    ADD CONSTRAINT ex_households_no_overlap
    EXCLUDE USING GIST (unit_id WITH =, kind WITH =, period WITH &&);

CREATE TABLE IF NOT EXISTS data.household_members (
                                                      id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id   UUID NOT NULL REFERENCES data.households(id) ON DELETE CASCADE,
    resident_id    UUID NOT NULL REFERENCES data.residents(id) ON DELETE RESTRICT,
    relation       TEXT,
    is_primary     BOOLEAN NOT NULL DEFAULT FALSE,
    joined_at      DATE,
    left_at        DATE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_member_unique UNIQUE (household_id, resident_id),
    CONSTRAINT ck_member_period CHECK (left_at IS NULL OR joined_at IS NULL OR left_at >= joined_at)
    );

CREATE TABLE IF NOT EXISTS data.unit_parties (
                                                 id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL,
    unit_id     UUID NOT NULL REFERENCES data.units(id) ON DELETE CASCADE,
    party_type  data.party_type NOT NULL,
    resident_id UUID REFERENCES data.residents(id),
    role        data.party_role NOT NULL,
    start_date  DATE NOT NULL,
    end_date    DATE,
    period      DATERANGE GENERATED ALWAYS AS (daterange(start_date, COALESCE(end_date, 'infinity'::date), '[]')) STORED,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_unit_parties_period CHECK (end_date IS NULL OR end_date >= start_date)
    );

ALTER TABLE data.unit_parties
    ADD CONSTRAINT ex_unit_parties_no_overlap
    EXCLUDE USING GIST (unit_id WITH =, role WITH =, period WITH &&);

CREATE TABLE IF NOT EXISTS data.vehicles (
                                             id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL,
    resident_id UUID REFERENCES data.residents(id),
    unit_id     UUID REFERENCES data.units(id) ON DELETE SET NULL,
    plate_no    TEXT NOT NULL,
    kind        data.vehicle_kind,
    color       TEXT,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_vehicle_tenant_plate UNIQUE (tenant_id, plate_no)
    );

CREATE TABLE IF NOT EXISTS data.meters (
                                           id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID NOT NULL,
    unit_id      UUID NOT NULL REFERENCES data.units(id) ON DELETE CASCADE,
    service_id   UUID NOT NULL REFERENCES svc.services(id) ON DELETE RESTRICT,
    meter_code   TEXT NOT NULL,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    installed_at DATE,
    removed_at   DATE,
    period       DATERANGE GENERATED ALWAYS AS (daterange(installed_at, COALESCE(removed_at, 'infinity'::date), '[]')) STORED,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_meter_tenant_code UNIQUE (tenant_id, meter_code),
    CONSTRAINT ck_meters_period CHECK (removed_at IS NULL OR removed_at >= installed_at)
    );

ALTER TABLE data.meters
    ADD CONSTRAINT ex_meters_no_overlap
    EXCLUDE USING GIST (unit_id WITH =, service_id WITH =, period WITH &&);

CREATE OR REPLACE VIEW data.v_current_households AS
SELECT h.*
FROM data.households h
WHERE h.end_date IS NULL OR h.end_date >= CURRENT_DATE;

CREATE OR REPLACE VIEW data.v_current_unit_parties AS
SELECT up.*
FROM data.unit_parties up
WHERE up.end_date IS NULL OR up.end_date >= CURRENT_DATE;

CREATE OR REPLACE VIEW data.v_active_vehicles AS
SELECT v.* FROM data.vehicles v WHERE v.active = TRUE;

CREATE INDEX IF NOT EXISTS idx_buildings_tenant ON data.Buildings (tenant_id);
CREATE INDEX IF NOT EXISTS idx_units_tenant_status ON data.units (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_units_building ON data.units (building_id);
CREATE INDEX IF NOT EXISTS idx_residents_tenant_status ON data.residents (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_residents_name_trgm ON data.residents USING GIN (full_name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_households_unit ON data.households (unit_id);
CREATE INDEX IF NOT EXISTS idx_households_period ON data.households USING GIST (period);
CREATE UNIQUE INDEX IF NOT EXISTS uq_household_one_primary ON data.household_members (household_id) WHERE is_primary = TRUE;
CREATE INDEX IF NOT EXISTS idx_household_members_resident ON data.household_members (resident_id);
CREATE INDEX IF NOT EXISTS idx_unit_parties_unit_role ON data.unit_parties (unit_id, role);
CREATE INDEX IF NOT EXISTS idx_unit_parties_resident ON data.unit_parties (resident_id);
CREATE INDEX IF NOT EXISTS idx_unit_parties_period ON data.unit_parties USING GIST (period);
CREATE INDEX IF NOT EXISTS idx_vehicles_resident ON data.vehicles (resident_id);
CREATE INDEX IF NOT EXISTS idx_vehicles_unit ON data.vehicles (unit_id);
CREATE INDEX IF NOT EXISTS idx_meters_unit ON data.meters (unit_id);
CREATE INDEX IF NOT EXISTS idx_meters_service ON data.meters (service_id);
CREATE INDEX IF NOT EXISTS idx_meters_period ON data.meters USING GIST (period);
