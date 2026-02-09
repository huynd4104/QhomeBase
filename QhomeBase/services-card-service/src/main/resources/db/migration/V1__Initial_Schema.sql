/*
 * V1__Initial_Schema.sql
 *
 * Initial database schema for Services Card Service.
 * This file consolidates previous migrations to provide a clean starting state.
 *
 * Features:
 * - Card Registrations (Resident, Elevator, Vehicle)
 * - Vehicle Registration Images
 * - Card Pricing Configuration
 * - Fee Reminder State Tracking
 *
 */

CREATE SCHEMA IF NOT EXISTS card;

-- =================================================================================================
-- 1. Configuration Tables
-- =================================================================================================

-- Table: card.card_pricing
-- Description: Configurable pricing for different card types.
CREATE TABLE IF NOT EXISTS card.card_pricing (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    card_type VARCHAR(50) NOT NULL UNIQUE, -- VEHICLE, RESIDENT, ELEVATOR
    price NUMERIC(14, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    CONSTRAINT chk_card_type CHECK (card_type IN ('VEHICLE', 'RESIDENT', 'ELEVATOR')),
    CONSTRAINT chk_price_positive CHECK (price > 0)
);

CREATE INDEX IF NOT EXISTS idx_card_pricing_type_active ON card.card_pricing(card_type, is_active);

COMMENT ON TABLE card.card_pricing IS 'Stores configurable card registration fees.';
COMMENT ON COLUMN card.card_pricing.card_type IS 'Card type: VEHICLE, RESIDENT, ELEVATOR';
COMMENT ON COLUMN card.card_pricing.price IS 'Registration fee (VND)';


-- =================================================================================================
-- 2. Card Registration Tables
-- =================================================================================================

-- Table: card.resident_card_registration
-- Description: Requests for Resident Cards.
CREATE TABLE IF NOT EXISTS card.resident_card_registration (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    unit_id UUID, -- Can be NULL initially, updated later
    resident_id UUID, -- Can be NULL initially
    request_type VARCHAR(50) NOT NULL DEFAULT 'NEW_CARD',
    
    -- Personal Info (Snapshot or Manual Entry)
    full_name TEXT,
    phone_number VARCHAR(20),
    citizen_id TEXT,
    apartment_number TEXT,
    building_name TEXT,
    
    -- Status & Admin
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    note TEXT,
    admin_note TEXT,
    approved_by UUID,
    approved_at TIMESTAMPTZ,
    rejection_reason TEXT,
    
    -- Payment Info
    payment_status VARCHAR(50) NOT NULL DEFAULT 'UNPAID',
    payment_amount NUMERIC(14, 2),
    payment_date TIMESTAMPTZ,
    payment_gateway VARCHAR(50),
    vnpay_transaction_ref VARCHAR(255),
    vnpay_initiated_at TIMESTAMPTZ,
    
    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_resident_card_user_id ON card.resident_card_registration(user_id);
CREATE INDEX IF NOT EXISTS idx_resident_card_unit_id ON card.resident_card_registration(unit_id);
CREATE INDEX IF NOT EXISTS idx_resident_card_resident_id ON card.resident_card_registration(resident_id);
CREATE INDEX IF NOT EXISTS idx_resident_card_status ON card.resident_card_registration(status);
CREATE INDEX IF NOT EXISTS idx_resident_card_payment_status ON card.resident_card_registration(payment_status);
CREATE INDEX IF NOT EXISTS idx_resident_card_vnpay_initiated ON card.resident_card_registration(vnpay_initiated_at) 
    WHERE vnpay_initiated_at IS NOT NULL AND payment_status = 'PAYMENT_IN_PROGRESS';


-- Table: card.elevator_card_registration
-- Description: Requests for Elevator Cards.
CREATE TABLE IF NOT EXISTS card.elevator_card_registration (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    unit_id UUID,
    resident_id UUID,
    request_type VARCHAR(50) NOT NULL DEFAULT 'NEW_CARD',
    
    -- Personal Info
    full_name TEXT,
    phone_number VARCHAR(20),
    citizen_id TEXT,
    apartment_number TEXT,
    building_name TEXT,
    
    -- Status & Admin
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    note TEXT,
    admin_note TEXT,
    approved_by UUID,
    approved_at TIMESTAMPTZ,
    rejection_reason TEXT,
    
    -- Payment Info
    payment_status VARCHAR(50) NOT NULL DEFAULT 'UNPAID',
    payment_amount NUMERIC(14, 2) DEFAULT 30000,
    payment_date TIMESTAMPTZ,
    payment_gateway VARCHAR(50),
    vnpay_transaction_ref VARCHAR(255),
    vnpay_initiated_at TIMESTAMPTZ,
    
    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_elevator_card_user_id ON card.elevator_card_registration(user_id);
CREATE INDEX IF NOT EXISTS idx_elevator_card_unit_id ON card.elevator_card_registration(unit_id);
CREATE INDEX IF NOT EXISTS idx_elevator_card_resident_id ON card.elevator_card_registration(resident_id);
CREATE INDEX IF NOT EXISTS idx_elevator_card_status ON card.elevator_card_registration(status);
CREATE INDEX IF NOT EXISTS idx_elevator_card_payment_status ON card.elevator_card_registration(payment_status);
CREATE INDEX IF NOT EXISTS idx_elevator_card_vnpay_initiated ON card.elevator_card_registration(vnpay_initiated_at) 
    WHERE vnpay_initiated_at IS NOT NULL AND payment_status = 'PAYMENT_IN_PROGRESS';


-- Table: card.register_vehicle
-- Description: Requests for Vehicle Cards/Registration.
CREATE TABLE IF NOT EXISTS card.register_vehicle (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    unit_id UUID,
    service_type VARCHAR(50) NOT NULL, -- e.g., VEHICLE_REGISTRATION
    request_type VARCHAR(50) DEFAULT 'NEW_CARD',
    
    -- Reissue Logic
    reissued_from_card_id UUID NULL,
    
    -- Vehicle Info
    vehicle_type VARCHAR(50),
    license_plate VARCHAR(20),
    vehicle_brand VARCHAR(100),
    vehicle_color VARCHAR(50),
    apartment_number TEXT,
    building_name TEXT,
    
    -- Status & Admin
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    note TEXT,
    admin_note TEXT,
    approved_by UUID,
    approved_at TIMESTAMPTZ,
    rejection_reason TEXT,
    
    -- Payment Info
    payment_status VARCHAR(50) DEFAULT 'UNPAID',
    payment_amount NUMERIC(14, 2),
    payment_date TIMESTAMPTZ,
    payment_gateway VARCHAR(50),
    vnpay_transaction_ref VARCHAR(255),
    vnpay_initiated_at TIMESTAMPTZ,
    
    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT fk_register_vehicle_reissued_from FOREIGN KEY (reissued_from_card_id) REFERENCES card.register_vehicle(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_register_vehicle_user_id ON card.register_vehicle(user_id);
CREATE INDEX IF NOT EXISTS idx_register_vehicle_status ON card.register_vehicle(status);
CREATE INDEX IF NOT EXISTS idx_register_vehicle_payment_status ON card.register_vehicle(payment_status);
CREATE INDEX IF NOT EXISTS idx_register_vehicle_service_type ON card.register_vehicle(service_type);
CREATE INDEX IF NOT EXISTS idx_vehicle_card_vnpay_initiated ON card.register_vehicle(vnpay_initiated_at) 
    WHERE vnpay_initiated_at IS NOT NULL AND payment_status = 'PAYMENT_IN_PROGRESS';
CREATE INDEX IF NOT EXISTS idx_vehicle_card_reissued_from ON card.register_vehicle(reissued_from_card_id) 
    WHERE reissued_from_card_id IS NOT NULL;
COMMENT ON COLUMN card.register_vehicle.reissued_from_card_id IS 'ID of original card if reissued. NULL if NEW_CARD';


-- Table: card.register_vehicle_image
-- Description: Stores images associated with vehicle registration requests.
CREATE TABLE IF NOT EXISTS card.register_vehicle_image (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    register_vehicle_id UUID NOT NULL,
    image_url TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_register_vehicle_image_vehicle 
        FOREIGN KEY (register_vehicle_id) 
        REFERENCES card.register_vehicle(id) 
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_register_vehicle_image_vehicle_id ON card.register_vehicle_image(register_vehicle_id);


-- =================================================================================================
-- 3. State Tracking Tables
-- =================================================================================================

-- Table: card.card_fee_reminder_state
-- Description: Tracks monthly fee reminder state for cards.
CREATE TABLE IF NOT EXISTS card.card_fee_reminder_state (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- References
    card_type VARCHAR(30) NOT NULL, -- 'RESIDENT', 'ELEVATOR', 'VEHICLE'
    card_id UUID NOT NULL,
    user_id UUID,
    unit_id UUID,
    resident_id UUID,
    
    -- Cached Info
    apartment_number VARCHAR(100),
    building_name VARCHAR(100),
    
    -- Cycle Info
    cycle_start_date DATE NOT NULL,
    next_due_date DATE NOT NULL,
    
    -- Tracking
    reminder_count INT NOT NULL DEFAULT 0,
    max_reminders INT NOT NULL DEFAULT 6, -- 1 initial + 5 grace
    last_reminded_at TIMESTAMPTZ,
    
    -- Audit
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT uq_card_fee_reminder UNIQUE (card_type, card_id),
    CONSTRAINT ck_card_type CHECK (card_type IN ('RESIDENT', 'ELEVATOR', 'VEHICLE')),
    CONSTRAINT ck_reminder_count CHECK (reminder_count >= 0),
    CONSTRAINT ck_max_reminders CHECK (max_reminders > 0)
);

CREATE INDEX IF NOT EXISTS idx_card_fee_reminder_due
    ON card.card_fee_reminder_state (next_due_date, reminder_count, last_reminded_at)
    WHERE reminder_count < max_reminders;

CREATE INDEX IF NOT EXISTS idx_card_fee_reminder_unit ON card.card_fee_reminder_state (unit_id, next_due_date);
CREATE INDEX IF NOT EXISTS idx_card_fee_reminder_resident ON card.card_fee_reminder_state (resident_id, next_due_date);
CREATE INDEX IF NOT EXISTS idx_card_fee_reminder_card ON card.card_fee_reminder_state (card_type, card_id);

COMMENT ON TABLE card.card_fee_reminder_state IS 'Tracks monthly fee reminder state. Separated from card tables for scalability.';
