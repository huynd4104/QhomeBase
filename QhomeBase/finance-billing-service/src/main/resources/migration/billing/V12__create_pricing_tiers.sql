CREATE TABLE IF NOT EXISTS billing.pricing_tiers (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_code        TEXT NOT NULL,
    tier_order          INTEGER NOT NULL,
    min_quantity        NUMERIC(14,3) NOT NULL DEFAULT 0,
    max_quantity        NUMERIC(14,3),
    unit_price          NUMERIC(14,4) NOT NULL,
    effective_from      DATE NOT NULL,
    effective_until     DATE,
    active              BOOLEAN NOT NULL DEFAULT true,
    description         TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          UUID,
    updated_by          UUID,
    
    CONSTRAINT ck_tier_order_positive CHECK (tier_order > 0),
    CONSTRAINT ck_min_max_quantity CHECK (max_quantity IS NULL OR max_quantity > min_quantity),
    CONSTRAINT ck_unit_price_positive CHECK (unit_price >= 0),
    CONSTRAINT ck_effective_date_range CHECK (effective_until IS NULL OR effective_until >= effective_from),
    CONSTRAINT uq_pricing_tier_service_order_date 
        UNIQUE (service_code, tier_order, effective_from)
);

CREATE INDEX IF NOT EXISTS idx_pricing_tiers_service_date 
    ON billing.pricing_tiers (service_code, effective_from DESC, effective_until DESC, active)
    WHERE active = true;

CREATE INDEX IF NOT EXISTS idx_pricing_tiers_service_order 
    ON billing.pricing_tiers (service_code, tier_order, effective_from DESC)
    WHERE active = true;

