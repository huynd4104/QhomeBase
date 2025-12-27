CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE SCHEMA IF NOT EXISTS data;

create table if not EXISTS data.tenants (
                                            id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        TEXT NOT NULL UNIQUE,
    name        TEXT NOT NULL,
    contact     TEXT,
    email       TEXT,
    status      TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()

    );


ALTER TABLE data.Buildings
    ADD CONSTRAINT fk_buildings_tenant FOREIGN KEY (tenant_id) REFERENCES data.tenants(id) ON DELETE CASCADE;

ALTER TABLE data.units
    ADD CONSTRAINT fk_units_tenant FOREIGN KEY (tenant_id) REFERENCES data.tenants(id) ON DELETE CASCADE;

ALTER TABLE data.residents
    ADD CONSTRAINT fk_residents_tenant FOREIGN KEY (tenant_id) REFERENCES data.tenants(id) ON DELETE CASCADE;

ALTER TABLE data.households
    ADD CONSTRAINT fk_households_tenant FOREIGN KEY (tenant_id) REFERENCES data.tenants(id) ON DELETE CASCADE;

ALTER TABLE data.unit_parties
    ADD CONSTRAINT fk_unit_parties_tenant FOREIGN KEY (tenant_id) REFERENCES data.tenants(id) ON DELETE CASCADE;

ALTER TABLE data.vehicles
    ADD CONSTRAINT fk_vehicles_tenant FOREIGN KEY (tenant_id) REFERENCES data.tenants(id) ON DELETE CASCADE;

ALTER TABLE data.meters
    ADD CONSTRAINT fk_meters_tenant FOREIGN KEY (tenant_id) REFERENCES data.tenants(id) ON DELETE CASCADE;
