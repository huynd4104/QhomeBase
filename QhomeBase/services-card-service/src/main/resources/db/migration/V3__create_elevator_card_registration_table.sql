CREATE TABLE IF NOT EXISTS card.elevator_card_registration (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    unit_id UUID NOT NULL,
    request_type VARCHAR(50) NOT NULL DEFAULT 'NEW_CARD',
    resident_id UUID NOT NULL,
    phone_number VARCHAR(20),
    note TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    payment_status VARCHAR(50) NOT NULL DEFAULT 'UNPAID',
    payment_amount NUMERIC(14, 2),
    payment_date TIMESTAMPTZ,
    payment_gateway VARCHAR(50),
    vnpay_transaction_ref VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_elevator_card_user_id ON card.elevator_card_registration(user_id);
CREATE INDEX IF NOT EXISTS idx_elevator_card_unit_id ON card.elevator_card_registration(unit_id);
CREATE INDEX IF NOT EXISTS idx_elevator_card_resident_id ON card.elevator_card_registration(resident_id);
CREATE INDEX IF NOT EXISTS idx_elevator_card_status ON card.elevator_card_registration(status);
CREATE INDEX IF NOT EXISTS idx_elevator_card_payment_status ON card.elevator_card_registration(payment_status);

