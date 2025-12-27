SET search_path TO data, svc, public;

ALTER TABLE data.meters
DROP CONSTRAINT IF EXISTS fk_meters_service;

ALTER TABLE data.meters
DROP CONSTRAINT IF EXISTS meters_service_id_fkey;

ALTER TABLE data.meters
ADD CONSTRAINT fk_meters_service 
FOREIGN KEY (service_id) REFERENCES data.services(id) ON DELETE RESTRICT;

DO $$
DECLARE
    electric_service_id UUID;
    water_service_id UUID;
    unit_a101_id UUID := '550e8400-e29b-41d4-a716-446655440010'::uuid;
    unit_a102_id UUID := '550e8400-e29b-41d4-a716-446655440011'::uuid;
    unit_a201_id UUID := '550e8400-e29b-41d4-a716-446655440012'::uuid;
    unit_b101_id UUID := '550e8400-e29b-41d4-a716-446655440020'::uuid;
BEGIN
    SELECT id INTO electric_service_id 
    FROM data.services 
    WHERE code = 'ELECTRIC' AND active = TRUE 
    LIMIT 1;
    
    SELECT id INTO water_service_id 
    FROM data.services 
    WHERE code = 'WATER' AND active = TRUE 
    LIMIT 1;
    
    IF electric_service_id IS NULL THEN
        RAISE EXCEPTION 'ELECTRIC service not found or not active in data.services. Please ensure V27 has run correctly.';
    END IF;
    
    IF water_service_id IS NULL THEN
        RAISE EXCEPTION 'WATER service not found or not active in data.services. Please ensure V27 has run correctly.';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM data.services WHERE id = electric_service_id) THEN
        RAISE EXCEPTION 'ELECTRIC service ID % not found in data.services', electric_service_id;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM data.services WHERE id = water_service_id) THEN
        RAISE EXCEPTION 'WATER service ID % not found in data.services', water_service_id;
    END IF;
    
    INSERT INTO data.meters (id, unit_id, service_id, meter_code, active, installed_at, created_at, updated_at)
    VALUES
        ('650e8400-e29b-41d4-a716-446655440010'::uuid, unit_a101_id, electric_service_id, 'ELEC-A101', true, '2023-01-01', now(), now()),
        ('650e8400-e29b-41d4-a716-446655440011'::uuid, unit_a101_id, water_service_id, 'WTR-A101', true, '2023-01-01', now(), now()),
        ('650e8400-e29b-41d4-a716-446655440020'::uuid, unit_a102_id, electric_service_id, 'ELEC-A102', true, '2023-01-01', now(), now()),
        ('650e8400-e29b-41d4-a716-446655440021'::uuid, unit_a102_id, water_service_id, 'WTR-A102', true, '2023-01-01', now(), now()),
        ('650e8400-e29b-41d4-a716-446655440030'::uuid, unit_a201_id, electric_service_id, 'ELEC-A201', true, '2023-01-01', now(), now()),
        ('650e8400-e29b-41d4-a716-446655440031'::uuid, unit_a201_id, water_service_id, 'WTR-A201', true, '2023-01-01', now(), now()),
        ('650e8400-e29b-41d4-a716-446655440040'::uuid, unit_b101_id, electric_service_id, 'ELEC-B101', true, '2023-01-01', now(), now()),
        ('650e8400-e29b-41d4-a716-446655440041'::uuid, unit_b101_id, water_service_id, 'WTR-B101', true, '2023-01-01', now(), now())
    ON CONFLICT (id) DO NOTHING;
END $$;
