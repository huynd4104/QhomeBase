CREATE SCHEMA IF NOT EXISTS svc;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS btree_gist;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid=t.typnamespace
    WHERE t.typname = 'formula_type' AND n.nspname = 'svc'
  ) THEN
CREATE TYPE svc.formula_type AS ENUM ('FLAT','TIER','EXPR');
END IF;
END$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid=t.typnamespace
    WHERE t.typname = 'card_status' AND n.nspname = 'svc'
  ) THEN
CREATE TYPE svc.card_status AS ENUM ('ACTIVE','LOCKED','REVOKED','EXPIRED');
END IF;
END$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid=t.typnamespace
    WHERE t.typname = 'card_type' AND n.nspname = 'svc'
  ) THEN
CREATE TYPE svc.card_type AS ENUM ('ELEVATOR','PARKING','ACCESS');
END IF;
END$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid=t.typnamespace
    WHERE t.typname = 'owner_type' AND n.nspname = 'svc'
  ) THEN
CREATE TYPE svc.owner_type AS ENUM ('RESIDENT','STAFF','GUEST','OTHER');
END IF;
END$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid=t.typnamespace
    WHERE t.typname = 'card_event_type' AND n.nspname = 'svc'
  ) THEN
CREATE TYPE svc.card_event_type AS ENUM (
      'ISSUED','REVOKED','LOCKED','UNLOCKED','EXTENDED',
      'PACKAGE_ADDED','PACKAGE_REMOVED','ACCESS','OTHER'
    );
END IF;
END$$;

CREATE TABLE IF NOT EXISTS svc.services (
                                            id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID NOT NULL,
    code       TEXT NOT NULL,
    name       TEXT NOT NULL,
    category   TEXT,
    unit       TEXT,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_services_tenant_code UNIQUE (tenant_id, code)
    );

CREATE TABLE IF NOT EXISTS svc.pricing_formulas (
                                                    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id            UUID NOT NULL,
    service_id           UUID NOT NULL REFERENCES svc.services(id) ON DELETE CASCADE,
    formula_type         svc.formula_type NOT NULL,
    formula_json         JSONB NOT NULL,
    effective_from       DATE NOT NULL,
    effective_to         DATE,
    effective_daterange  DATERANGE GENERATED ALWAYS AS (daterange(effective_from, COALESCE(effective_to, 'infinity'::date), '[]')) STORED
    );

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'ex_pricing_formulas_no_overlap'
      AND connamespace = 'svc'::regnamespace
  ) THEN
ALTER TABLE svc.pricing_formulas
    ADD CONSTRAINT ex_pricing_formulas_no_overlap
    EXCLUDE USING gist (
        tenant_id WITH =,
        service_id WITH =,
        effective_daterange WITH &&
      );
END IF;
END$$;

CREATE TABLE IF NOT EXISTS svc.pricing_tiers (
                                                 id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pricing_formula_id  UUID NOT NULL REFERENCES svc.pricing_formulas(id) ON DELETE CASCADE,
    min_value           NUMERIC(14,4) NOT NULL,
    max_value           NUMERIC(14,4),
    price_per_unit      NUMERIC(14,4) NOT NULL CHECK (price_per_unit >= 0),
    CONSTRAINT ck_tier_range CHECK (max_value IS NULL OR min_value <= max_value)
    );

CREATE TABLE IF NOT EXISTS svc.cards (
                                         id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID NOT NULL,
    card_no    TEXT NOT NULL,
    type       svc.card_type NOT NULL,
    owner_type svc.owner_type NOT NULL,
    owner_id   UUID,
    status     svc.card_status NOT NULL DEFAULT 'ACTIVE',
    issued_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    expired_at TIMESTAMPTZ,
    CONSTRAINT uq_cards_tenant_cardno UNIQUE (tenant_id, card_no)
    );

CREATE TABLE IF NOT EXISTS svc.card_packages (
                                                 id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL,
    name        TEXT NOT NULL,
    rights_json JSONB NOT NULL,
    CONSTRAINT uq_card_packages_tenant_name UNIQUE (tenant_id, name)
    );

CREATE TABLE IF NOT EXISTS svc.card_assignments (
                                                    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL,
    card_id     UUID NOT NULL REFERENCES svc.cards(id) ON DELETE CASCADE,
    package_id  UUID NOT NULL REFERENCES svc.card_packages(id) ON DELETE RESTRICT,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_card_assign UNIQUE (tenant_id, card_id, package_id)
    );

CREATE TABLE IF NOT EXISTS svc.card_events (
                                               id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID NOT NULL,
    card_id    UUID NOT NULL REFERENCES svc.cards(id) ON DELETE CASCADE,
    event_type svc.card_event_type NOT NULL,
    actor_id   UUID,
    note       TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_services_tenant_active ON svc.services (tenant_id) WHERE active = TRUE;
CREATE INDEX IF NOT EXISTS idx_pricing_formulas_tenant_service ON svc.pricing_formulas (tenant_id, service_id);
CREATE INDEX IF NOT EXISTS idx_tiers_formula_min ON svc.pricing_tiers (pricing_formula_id, min_value);
CREATE INDEX IF NOT EXISTS idx_cards_tenant_status ON svc.cards (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_cards_owner ON svc.cards (tenant_id, owner_type, owner_id);
CREATE INDEX IF NOT EXISTS idx_card_assign_card_time ON svc.card_assignments (card_id, assigned_at DESC);
CREATE INDEX IF NOT EXISTS idx_card_events_card_time ON svc.card_events (card_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_card_events_tenant_type_time ON svc.card_events (tenant_id, event_type, created_at DESC);
