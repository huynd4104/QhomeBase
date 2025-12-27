-- Insert test tenant for building creation testing
INSERT INTO data.tenants (id, code, name, contact, email, status, created_at, updated_at, created_by, is_deleted, description)
VALUES (
    '2b5b2af5-9431-4649-8144-35830d866826',  -- tenant_id từ JWT token
    'TEST',                                    -- tenant code
    'Test Tenant',                             -- tenant name
    '0123456789',                              -- contact
    'test@example.com',                        -- email
    'ACTIVE',                                  -- status
    NOW(),                                     -- created_at
    NOW(),                                     -- updated_at
    'system',                                  -- created_by
    false,                                     -- is_deleted
    'Test tenant for building creation'        -- description
) ON CONFLICT (id) DO NOTHING;
