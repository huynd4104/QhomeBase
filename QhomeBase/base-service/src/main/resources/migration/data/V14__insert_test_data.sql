-- Insert test data for building deletion testing
-- This script inserts sample data needed for testing building deletion APIs

-- Insert test tenant (if not exists)
INSERT INTO data.tenants (id, name, code, status, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440001'::uuid,
    'Test Tenant Company',
    'TEST_TENANT',
    'ACTIVE',
    now(),
    now()
) ON CONFLICT (id) DO NOTHING;

-- Insert test building (if not exists)
INSERT INTO data.Buildings (id, tenant_id, code, name, address, status, created_at, updated_at, is_deleted, created_by)
VALUES (
    '550e8400-e29b-41d4-a716-446655440003'::uuid,
    '550e8400-e29b-41d4-a716-446655440001'::uuid,
    'BLD001',
    'Test Building 1',
    '123 Test Street, Test City',
    'ACTIVE',
    now(),
    now(),
    false,
    'system'
) ON CONFLICT (id) DO NOTHING;

-- Insert test building 2 (for additional testing)
INSERT INTO data.Buildings (id, tenant_id, code, name, address, status, created_at, updated_at, is_deleted, created_by)
VALUES (
    '550e8400-e29b-41d4-a716-446655440004'::uuid,
    '550e8400-e29b-41d4-a716-446655440001'::uuid,
    'BLD002',
    'Test Building 2',
    '456 Test Avenue, Test City',
    'ACTIVE',
    now(),
    now(),
    false,
    'system'
) ON CONFLICT (id) DO NOTHING;

-- Insert test units for building 1
INSERT INTO data.units (id, tenant_id, building_id, code, floor, area_m2, bedrooms, status, created_at, updated_at)
VALUES 
    ('550e8400-e29b-41d4-a716-446655440010'::uuid, '550e8400-e29b-41d4-a716-446655440001'::uuid, '550e8400-e29b-41d4-a716-446655440003'::uuid, 'A101', 1, 50.5, 2, 'ACTIVE', now(), now()),
    ('550e8400-e29b-41d4-a716-446655440011'::uuid, '550e8400-e29b-41d4-a716-446655440001'::uuid, '550e8400-e29b-41d4-a716-446655440003'::uuid, 'A102', 1, 45.0, 1, 'ACTIVE', now(), now()),
    ('550e8400-e29b-41d4-a716-446655440012'::uuid, '550e8400-e29b-41d4-a716-446655440001'::uuid, '550e8400-e29b-41d4-a716-446655440003'::uuid, 'A201', 2, 60.0, 3, 'ACTIVE', now(), now())
ON CONFLICT (id) DO NOTHING;

-- Insert test units for building 2
INSERT INTO data.units (id, tenant_id, building_id, code, floor, area_m2, bedrooms, status, created_at, updated_at)
VALUES 
    ('550e8400-e29b-41d4-a716-446655440020'::uuid, '550e8400-e29b-41d4-a716-446655440001'::uuid, '550e8400-e29b-41d4-a716-446655440004'::uuid, 'B101', 1, 55.0, 2, 'ACTIVE', now(), now()),
    ('550e8400-e29b-41d4-a716-446655440021'::uuid, '550e8400-e29b-41d4-a716-446655440001'::uuid, '550e8400-e29b-41d4-a716-446655440004'::uuid, 'B102', 1, 48.5, 1, 'INACTIVE', now(), now())
ON CONFLICT (id) DO NOTHING;
