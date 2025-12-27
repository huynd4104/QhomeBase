-- Create a separate table for card fee reminder state
-- This approach is more scalable and maintainable:
-- 1. Separates reminder logic from card entity (Single Responsibility)
-- 2. Easy to extend with new reminder types or metadata
-- 3. No need to modify card tables when adding new reminder features
-- 4. Can track reminder history and statistics

CREATE TABLE IF NOT EXISTS card.card_fee_reminder_state (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Card reference (polymorphic: any card type)
    card_type VARCHAR(30) NOT NULL, -- 'RESIDENT', 'ELEVATOR', 'VEHICLE'
    card_id UUID NOT NULL,
    
    -- Unit/Resident info (cached for quick querying)
    unit_id UUID,
    resident_id UUID,
    user_id UUID,
    apartment_number VARCHAR(100),
    building_name VARCHAR(100),
    
    -- Reminder cycle tracking
    cycle_start_date DATE NOT NULL,
    next_due_date DATE NOT NULL,
    
    -- Reminder tracking
    reminder_count INT NOT NULL DEFAULT 0,
    max_reminders INT NOT NULL DEFAULT 6, -- Configurable: 1 initial + 5 grace days
    last_reminded_at TIMESTAMPTZ,
    
    -- Timestamps
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    -- Constraints
    CONSTRAINT uq_card_fee_reminder UNIQUE (card_type, card_id),
    CONSTRAINT ck_card_type CHECK (card_type IN ('RESIDENT', 'ELEVATOR', 'VEHICLE')),
    CONSTRAINT ck_reminder_count CHECK (reminder_count >= 0),
    CONSTRAINT ck_max_reminders CHECK (max_reminders > 0)
);

-- Indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_card_fee_reminder_due
    ON card.card_fee_reminder_state (next_due_date, reminder_count, last_reminded_at)
    WHERE reminder_count < max_reminders;

CREATE INDEX IF NOT EXISTS idx_card_fee_reminder_unit
    ON card.card_fee_reminder_state (unit_id, next_due_date);

CREATE INDEX IF NOT EXISTS idx_card_fee_reminder_resident
    ON card.card_fee_reminder_state (resident_id, next_due_date);

CREATE INDEX IF NOT EXISTS idx_card_fee_reminder_card
    ON card.card_fee_reminder_state (card_type, card_id);

COMMENT ON TABLE card.card_fee_reminder_state IS 
    'Tracks monthly fee reminder state for all card types. Separated from card tables for better scalability and extensibility.';

COMMENT ON COLUMN card.card_fee_reminder_state.card_type IS 
    'Type of card: RESIDENT, ELEVATOR, or VEHICLE';

COMMENT ON COLUMN card.card_fee_reminder_state.cycle_start_date IS 
    'Start date of current payment cycle (usually payment_date or approved_at)';

COMMENT ON COLUMN card.card_fee_reminder_state.next_due_date IS 
    'Next due date for payment (cycle_start_date + 30 days)';

COMMENT ON COLUMN card.card_fee_reminder_state.reminder_count IS 
    'Number of reminders sent in current cycle';

COMMENT ON COLUMN card.card_fee_reminder_state.max_reminders IS 
    'Maximum reminders allowed per cycle (default: 6 = 1 initial + 5 grace days)';

