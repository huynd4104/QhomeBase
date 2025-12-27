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

