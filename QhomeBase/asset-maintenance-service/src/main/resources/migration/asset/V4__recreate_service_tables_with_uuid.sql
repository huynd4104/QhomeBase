-- V4: Recreate service-related tables with UUID primary keys
-- This migration drops the previously created tables (if any) and recreates them using UUID identifiers.

-- Drop dependent tables first due to foreign keys
DROP TABLE IF EXISTS asset.service_kpi_value CASCADE;
DROP TABLE IF EXISTS asset.service_kpi_target CASCADE;
DROP TABLE IF EXISTS asset.service_kpi_metric CASCADE;
DROP TABLE IF EXISTS asset.service_booking_slot CASCADE;
DROP TABLE IF EXISTS asset.service_booking_item CASCADE;
DROP TABLE IF EXISTS asset.service_booking CASCADE;
DROP TABLE IF EXISTS asset.service_ticket CASCADE;
DROP TABLE IF EXISTS asset.service_combo_item CASCADE;
DROP TABLE IF EXISTS asset.service_option_group_item CASCADE;
DROP TABLE IF EXISTS asset.service_option_group CASCADE;
DROP TABLE IF EXISTS asset.service_option CASCADE;
DROP TABLE IF EXISTS asset.service_combo CASCADE;
DROP TABLE IF EXISTS asset.service_availability CASCADE;
DROP TABLE IF EXISTS asset.service CASCADE;
DROP TABLE IF EXISTS asset.service_category CASCADE;

-- Recreate tables using UUID PKs

CREATE TABLE IF NOT EXISTS asset.service_category (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(50)  NOT NULL UNIQUE,
    name            VARCHAR(150) NOT NULL,
    description     TEXT,
    icon            VARCHAR(255),
    sort_order      INTEGER      NOT NULL DEFAULT 0,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE asset.service_category IS 'Categorises resident-facing services/amenities (spa, gym, BBQ, etc.)';

CREATE TABLE IF NOT EXISTS asset.service (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id            UUID         NOT NULL REFERENCES asset.service_category(id) ON DELETE RESTRICT,
    code                   VARCHAR(100) NOT NULL UNIQUE,
    name                   VARCHAR(200) NOT NULL,
    description            TEXT,
    location               VARCHAR(255),
    map_url                TEXT,
    price_per_hour         NUMERIC(15,2),
    price_per_session      NUMERIC(15,2),
    pricing_type           VARCHAR(50)  NOT NULL DEFAULT 'HOURLY',
    booking_type           VARCHAR(50),
    max_capacity           INTEGER,
    min_duration_hours     INTEGER      NOT NULL DEFAULT 1,
    max_duration_hours     INTEGER,
    rules                  TEXT,
    is_active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_service_pricing_type CHECK (pricing_type IN ('HOURLY', 'SESSION', 'FREE')),
    CONSTRAINT chk_service_booking_type CHECK (booking_type IS NULL OR booking_type IN ('COMBO_BASED', 'TICKET_BASED', 'OPTION_BASED', 'STANDARD')),
    CONSTRAINT chk_service_duration CHECK (max_duration_hours IS NULL OR max_duration_hours >= min_duration_hours)
);

CREATE INDEX IF NOT EXISTS idx_service_category ON asset.service(category_id);
CREATE INDEX IF NOT EXISTS idx_service_active ON asset.service(is_active);

CREATE TABLE IF NOT EXISTS asset.service_availability (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id     UUID        NOT NULL REFERENCES asset.service(id) ON DELETE CASCADE,
    day_of_week    SMALLINT    NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time     TIME        NOT NULL,
    end_time       TIME        NOT NULL,
    is_available   BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_service_availability_time CHECK (end_time > start_time)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_service_availability_slot
    ON asset.service_availability(service_id, day_of_week, start_time, end_time);

CREATE TABLE IF NOT EXISTS asset.service_combo (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id         UUID         NOT NULL REFERENCES asset.service(id) ON DELETE CASCADE,
    code               VARCHAR(100) NOT NULL,
    name               VARCHAR(200) NOT NULL,
    description        TEXT,
    services_included  TEXT,
    duration_minutes   INTEGER,
    price              NUMERIC(15,2) NOT NULL,
    is_active          BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order         INTEGER      NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_service_combo_duration CHECK (duration_minutes IS NULL OR duration_minutes > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_service_combo_code
    ON asset.service_combo(service_id, code);

CREATE TABLE IF NOT EXISTS asset.service_option (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id    UUID         NOT NULL REFERENCES asset.service(id) ON DELETE CASCADE,
    code          VARCHAR(100) NOT NULL,
    name          VARCHAR(200) NOT NULL,
    description   TEXT,
    price         NUMERIC(15,2) NOT NULL,
    unit          VARCHAR(100),
    is_required   BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order    INTEGER      NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_service_option_code
    ON asset.service_option(service_id, code);

CREATE TABLE IF NOT EXISTS asset.service_option_group (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id     UUID        NOT NULL REFERENCES asset.service(id) ON DELETE CASCADE,
    code           VARCHAR(100) NOT NULL,
    name           VARCHAR(200) NOT NULL,
    description    TEXT,
    min_select     INTEGER      NOT NULL DEFAULT 0,
    max_select     INTEGER,
    is_required    BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order     INTEGER      NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_service_option_group_limits CHECK (max_select IS NULL OR max_select >= min_select)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_service_option_group_code
    ON asset.service_option_group(service_id, code);

CREATE TABLE IF NOT EXISTS asset.service_option_group_item (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id       UUID        NOT NULL REFERENCES asset.service_option_group(id) ON DELETE CASCADE,
    option_id      UUID        NOT NULL REFERENCES asset.service_option(id) ON DELETE CASCADE,
    sort_order     INTEGER     NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(group_id, option_id)
);

CREATE INDEX IF NOT EXISTS idx_service_option_group_item_group
    ON asset.service_option_group_item(group_id);

CREATE TABLE IF NOT EXISTS asset.service_combo_item (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    combo_id            UUID        NOT NULL REFERENCES asset.service_combo(id) ON DELETE CASCADE,
    included_service_id UUID        REFERENCES asset.service(id) ON DELETE SET NULL,
    option_id           UUID        REFERENCES asset.service_option(id) ON DELETE SET NULL,
    quantity            INTEGER     NOT NULL DEFAULT 1,
    note                TEXT,
    sort_order          INTEGER     NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_service_combo_item_quantity CHECK (quantity > 0),
    CONSTRAINT chk_service_combo_item_reference CHECK (
        included_service_id IS NOT NULL OR option_id IS NOT NULL
    )
);

CREATE INDEX IF NOT EXISTS idx_service_combo_item_combo
    ON asset.service_combo_item(combo_id);

CREATE TABLE IF NOT EXISTS asset.service_ticket (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id     UUID         NOT NULL REFERENCES asset.service(id) ON DELETE CASCADE,
    code           VARCHAR(100) NOT NULL,
    name           VARCHAR(200) NOT NULL,
    ticket_type    VARCHAR(50)  NOT NULL,
    duration_hours NUMERIC(5,2),
    price          NUMERIC(15,2) NOT NULL,
    max_people     INTEGER,
    description    TEXT,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order     INTEGER      NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_service_ticket_type CHECK (ticket_type IN ('DAY', 'NIGHT', 'HOURLY', 'DAILY', 'FAMILY')),
    CONSTRAINT chk_service_ticket_duration CHECK (duration_hours IS NULL OR duration_hours > 0),
    CONSTRAINT chk_service_ticket_people CHECK (max_people IS NULL OR max_people > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_service_ticket_code
    ON asset.service_ticket(service_id, code);

CREATE TABLE IF NOT EXISTS asset.service_booking (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id           UUID         NOT NULL REFERENCES asset.service(id) ON DELETE RESTRICT,
    booking_date         DATE         NOT NULL,
    start_time           TIME         NOT NULL,
    end_time             TIME         NOT NULL,
    duration_hours       NUMERIC(5,2) NOT NULL,
    number_of_people     INTEGER      DEFAULT 1,
    purpose              TEXT,
    total_amount         NUMERIC(15,2) NOT NULL,
    payment_status       VARCHAR(50)  NOT NULL DEFAULT 'UNPAID',
    payment_date         TIMESTAMPTZ,
    payment_gateway      VARCHAR(100),
    vnpay_transaction_ref VARCHAR(100),
    status               VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    user_id              UUID         NOT NULL REFERENCES iam.users(id) ON DELETE RESTRICT,
    approved_by          UUID         REFERENCES iam.users(id) ON DELETE SET NULL,
    approved_at          TIMESTAMPTZ,
    rejection_reason     TEXT,
    terms_accepted       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_service_booking_times CHECK (end_time > start_time),
    CONSTRAINT chk_service_booking_duration CHECK (duration_hours > 0),
    CONSTRAINT chk_service_booking_people CHECK (number_of_people IS NULL OR number_of_people > 0),
    CONSTRAINT chk_service_booking_payment_status CHECK (payment_status IN ('UNPAID', 'PAID', 'PENDING', 'CANCELLED')),
    CONSTRAINT chk_service_booking_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_service_booking_service ON asset.service_booking(service_id);
CREATE INDEX IF NOT EXISTS idx_service_booking_user ON asset.service_booking(user_id);
CREATE INDEX IF NOT EXISTS idx_service_booking_status ON asset.service_booking(status);
CREATE INDEX IF NOT EXISTS idx_service_booking_date ON asset.service_booking(booking_date);

CREATE TABLE IF NOT EXISTS asset.service_booking_item (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id    UUID         NOT NULL REFERENCES asset.service_booking(id) ON DELETE CASCADE,
    item_type     VARCHAR(20)  NOT NULL,
    item_id       UUID         NOT NULL,
    item_code     VARCHAR(100) NOT NULL,
    item_name     VARCHAR(200) NOT NULL,
    quantity      INTEGER      NOT NULL DEFAULT 1,
    unit_price    NUMERIC(15,2) NOT NULL,
    total_price   NUMERIC(15,2) NOT NULL,
    metadata      JSONB,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_service_booking_item_type CHECK (item_type IN ('OPTION', 'COMBO', 'TICKET')),
    CONSTRAINT chk_service_booking_item_qty CHECK (quantity > 0),
    CONSTRAINT chk_service_booking_item_prices CHECK (unit_price >= 0 AND total_price >= 0)
);

CREATE INDEX IF NOT EXISTS idx_service_booking_item_booking
    ON asset.service_booking_item(booking_id);

CREATE TABLE IF NOT EXISTS asset.service_booking_slot (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id  UUID        NOT NULL REFERENCES asset.service_booking(id) ON DELETE CASCADE,
    service_id  UUID        NOT NULL REFERENCES asset.service(id) ON DELETE CASCADE,
    slot_date   DATE        NOT NULL,
    start_time  TIME        NOT NULL,
    end_time    TIME        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_service_booking_slot_time CHECK (end_time > start_time)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_service_booking_slot
    ON asset.service_booking_slot(service_id, slot_date, start_time, end_time);

CREATE INDEX IF NOT EXISTS idx_service_booking_slot_booking
    ON asset.service_booking_slot(booking_id);

COMMENT ON TABLE asset.service IS 'Configurable services/amenities available for residents to book (spa, BBQ, pool, etc.)';
COMMENT ON COLUMN asset.service.pricing_type IS 'Pricing model: HOURLY, SESSION, or FREE';
COMMENT ON COLUMN asset.service.booking_type IS 'Booking experience: COMBO_BASED, TICKET_BASED, OPTION_BASED, STANDARD';
COMMENT ON TABLE asset.service_booking IS 'Tracks resident bookings, approval workflow, payment status, and timing for services';
COMMENT ON TABLE asset.service_booking_item IS 'Line items representing combos/options/tickets associated with a booking';
COMMENT ON TABLE asset.service_booking_slot IS 'Resolved time slots booked for a service, used for capacity checks';

CREATE TABLE IF NOT EXISTS asset.service_kpi_metric (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id         UUID REFERENCES asset.service(id) ON DELETE SET NULL,
    code               VARCHAR(100) NOT NULL,
    name               VARCHAR(200) NOT NULL,
    description        TEXT,
    unit               VARCHAR(50),
    frequency          VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    calculation_method TEXT,
    is_active          BOOLEAN     NOT NULL DEFAULT TRUE,
    sort_order         INTEGER     NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_service_kpi_frequency CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY')),
    CONSTRAINT uq_service_kpi_metric_code UNIQUE (service_id, code)
);

CREATE INDEX IF NOT EXISTS idx_service_kpi_metric_service
    ON asset.service_kpi_metric(service_id);

CREATE TABLE IF NOT EXISTS asset.service_kpi_target (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_id           UUID        NOT NULL REFERENCES asset.service_kpi_metric(id) ON DELETE CASCADE,
    service_id          UUID REFERENCES asset.service(id) ON DELETE SET NULL,
    target_period_start DATE        NOT NULL,
    target_period_end   DATE        NOT NULL,
    target_value        NUMERIC(15,2),
    threshold_warning   NUMERIC(15,2),
    threshold_critical  NUMERIC(15,2),
    assigned_to         UUID REFERENCES iam.users(id) ON DELETE SET NULL,
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_service_kpi_target_period CHECK (target_period_end >= target_period_start),
    CONSTRAINT uq_service_kpi_target UNIQUE (metric_id, target_period_start, target_period_end)
);

CREATE INDEX IF NOT EXISTS idx_service_kpi_target_service
    ON asset.service_kpi_target(service_id);

CREATE TABLE IF NOT EXISTS asset.service_kpi_value (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_id     UUID        NOT NULL REFERENCES asset.service_kpi_metric(id) ON DELETE CASCADE,
    service_id    UUID REFERENCES asset.service(id) ON DELETE SET NULL,
    period_start  DATE        NOT NULL,
    period_end    DATE        NOT NULL,
    actual_value  NUMERIC(15,2),
    variance      NUMERIC(15,2),
    status        VARCHAR(20) NOT NULL DEFAULT 'FINAL',
    source        VARCHAR(20) NOT NULL DEFAULT 'SYSTEM',
    recorded_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    recorded_by   UUID REFERENCES iam.users(id) ON DELETE SET NULL,
    notes         TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_service_kpi_value_period CHECK (period_end >= period_start),
    CONSTRAINT chk_service_kpi_value_status CHECK (status IN ('DRAFT', 'FINAL')),
    CONSTRAINT chk_service_kpi_value_source CHECK (source IN ('SYSTEM', 'MANUAL')),
    CONSTRAINT uq_service_kpi_value UNIQUE (metric_id, period_start, period_end)
);

CREATE INDEX IF NOT EXISTS idx_service_kpi_value_service
    ON asset.service_kpi_value(service_id);

COMMENT ON TABLE asset.service_kpi_metric IS 'Defines KPI metrics to monitor service performance (usage, revenue, cancellation rate, etc.)';
COMMENT ON TABLE asset.service_kpi_target IS 'Stores target values for KPI metrics for given periods';
COMMENT ON TABLE asset.service_kpi_value IS 'Stores actual KPI results captured per period';

