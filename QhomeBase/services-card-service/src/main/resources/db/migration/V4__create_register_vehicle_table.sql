CREATE TABLE IF NOT EXISTS card.register_vehicle (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    service_type VARCHAR(50) NOT NULL,
    request_type VARCHAR(50) DEFAULT 'NEW_CARD',
    note TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    vehicle_type VARCHAR(50),
    license_plate VARCHAR(20),
    vehicle_brand VARCHAR(100),
    vehicle_color VARCHAR(50),
    payment_status VARCHAR(50) DEFAULT 'UNPAID',
    payment_amount NUMERIC(14, 2),
    payment_date TIMESTAMPTZ,
    payment_gateway VARCHAR(50),
    vnpay_transaction_ref VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_register_vehicle_user_id ON card.register_vehicle(user_id);
CREATE INDEX IF NOT EXISTS idx_register_vehicle_status ON card.register_vehicle(status);
CREATE INDEX IF NOT EXISTS idx_register_vehicle_payment_status ON card.register_vehicle(payment_status);
CREATE INDEX IF NOT EXISTS idx_register_vehicle_service_type ON card.register_vehicle(service_type);

