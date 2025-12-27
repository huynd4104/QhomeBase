-- V23: Add meter reading workflow tables

-- Table to manage reading cycles/periods
CREATE TABLE IF NOT EXISTS data.reading_cycles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            TEXT NOT NULL,
    period_from     DATE NOT NULL,
    period_to       DATE NOT NULL,
    status          TEXT NOT NULL DEFAULT 'OPEN',
    description     TEXT,
    created_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT uq_reading_cycle_name UNIQUE (name),
    CONSTRAINT ck_reading_cycle_period CHECK (period_from <= period_to),
    CONSTRAINT ck_reading_cycle_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'COMPLETED', 'CLOSED'))
);

-- Table to assign meter reading tasks to staff
CREATE TABLE IF NOT EXISTS data.meter_reading_assignments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cycle_id        UUID NOT NULL,
    building_id     UUID,
    service_id      UUID NOT NULL,
    assigned_to     UUID NOT NULL,
    assigned_by     UUID NOT NULL,
    assigned_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    due_date        DATE,
    completed_at    TIMESTAMPTZ,
    note            TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT fk_assignment_cycle
        FOREIGN KEY (cycle_id) REFERENCES data.reading_cycles(id) ON DELETE CASCADE,
    CONSTRAINT fk_assignment_building
        FOREIGN KEY (building_id) REFERENCES data.buildings(id) ON DELETE CASCADE,
    CONSTRAINT fk_assignment_service
        FOREIGN KEY (service_id) REFERENCES svc.services(id) ON DELETE RESTRICT
);

-- Table to track meter reading sessions (batch reading)
CREATE TABLE IF NOT EXISTS data.meter_reading_sessions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cycle_id        UUID NOT NULL,
    building_id     UUID,
    service_id      UUID NOT NULL,
    reader_id       UUID NOT NULL,
    started_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at    TIMESTAMPTZ,
    units_read      INTEGER DEFAULT 0,
    status          TEXT NOT NULL DEFAULT 'IN_PROGRESS',
    device_info     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT fk_session_cycle
        FOREIGN KEY (cycle_id) REFERENCES data.reading_cycles(id) ON DELETE CASCADE,
    CONSTRAINT fk_session_building
        FOREIGN KEY (building_id) REFERENCES data.buildings(id) ON DELETE CASCADE,
    CONSTRAINT fk_session_service
        FOREIGN KEY (service_id) REFERENCES svc.services(id) ON DELETE RESTRICT,
    CONSTRAINT ck_session_status
        CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);

-- Table to store meter readings
CREATE TABLE IF NOT EXISTS data.meter_readings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    meter_id        UUID NOT NULL,
    session_id      UUID,
    reading_date    DATE NOT NULL,
    prev_index      NUMERIC(14,3) NOT NULL,
    curr_index      NUMERIC(14,3) NOT NULL,
    consumption     NUMERIC(14,3) GENERATED ALWAYS AS (curr_index - prev_index) STORED,
    photo_file_id   UUID,
    note            TEXT,
    reader_id       UUID NOT NULL,
    read_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    verified        BOOLEAN DEFAULT false,
    verified_by     UUID,
    verified_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT fk_reading_meter
        FOREIGN KEY (meter_id) REFERENCES data.meters(id) ON DELETE CASCADE,
    CONSTRAINT fk_reading_session
        FOREIGN KEY (session_id) REFERENCES data.meter_reading_sessions(id) ON DELETE SET NULL,
    CONSTRAINT ck_reading_nonneg 
        CHECK (curr_index >= 0 AND prev_index >= 0 AND curr_index >= prev_index),
    CONSTRAINT uq_meter_reading 
        UNIQUE (meter_id, reading_date)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_reading_cycles_status ON data.reading_cycles (status, period_from DESC);
CREATE INDEX IF NOT EXISTS idx_reading_cycles_period ON data.reading_cycles (period_from, period_to);

CREATE INDEX IF NOT EXISTS idx_assignments_cycle ON data.meter_reading_assignments (cycle_id);
CREATE INDEX IF NOT EXISTS idx_assignments_assigned_to ON data.meter_reading_assignments (assigned_to, completed_at);
CREATE INDEX IF NOT EXISTS idx_assignments_building_service ON data.meter_reading_assignments (building_id, service_id);
CREATE INDEX IF NOT EXISTS idx_assignments_due_date ON data.meter_reading_assignments (due_date, completed_at);

CREATE INDEX IF NOT EXISTS idx_sessions_cycle ON data.meter_reading_sessions (cycle_id, status);
CREATE INDEX IF NOT EXISTS idx_sessions_reader ON data.meter_reading_sessions (reader_id, status);
CREATE INDEX IF NOT EXISTS idx_sessions_building ON data.meter_reading_sessions (building_id, status);
CREATE INDEX IF NOT EXISTS idx_sessions_started ON data.meter_reading_sessions (started_at DESC);

CREATE INDEX IF NOT EXISTS idx_readings_meter ON data.meter_readings (meter_id, reading_date DESC);
CREATE INDEX IF NOT EXISTS idx_readings_session ON data.meter_readings (session_id);
CREATE INDEX IF NOT EXISTS idx_readings_reader ON data.meter_readings (reader_id);
CREATE INDEX IF NOT EXISTS idx_readings_verified ON data.meter_readings (verified, verified_at);
CREATE INDEX IF NOT EXISTS idx_readings_date ON data.meter_readings (reading_date DESC);

