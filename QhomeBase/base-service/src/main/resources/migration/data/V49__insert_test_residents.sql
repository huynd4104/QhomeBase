-- V49: Insert test residents for account creation workflow testing
-- This script inserts sample residents with different statuses and scenarios

-- Insert residents for testing account creation workflow
-- Note: All residents have user_id = NULL initially
-- User accounts will be created via approval workflow or manually linked later
-- After iam-service migration V22 runs, you can update user_id for primary residents

-- Resident 1: Primary resident (chủ hộ) - Unit A101 - Will have account (user_id to be linked after iam-service runs)
INSERT INTO data.residents (id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440100'::uuid,
    'Nguyễn Văn A',
    '0912345678',
    'nguyenvana@example.com',
    '001234567890',
    '1980-01-15',
    'ACTIVE',
    NULL, -- Will be linked to user: nguyenvana (550e8400-e29b-41d4-a716-446655440110) after iam-service migration V22 runs
    now(),
    now()
) ON CONFLICT (id) DO UPDATE 
SET full_name = EXCLUDED.full_name,
    phone = EXCLUDED.phone,
    email = EXCLUDED.email,
    national_id = EXCLUDED.national_id,
    dob = EXCLUDED.dob,
    status = EXCLUDED.status,
    updated_at = now();

-- Resident 2: Family member - Unit A101 - No account yet
INSERT INTO data.residents (id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440101'::uuid,
    'Nguyễn Thị B',
    '0912345679',
    'nguyenthib@example.com',
    '001234567891',
    '1985-03-20',
    'ACTIVE',
    NULL, -- No account yet
    now(),
    now()
) ON CONFLICT (id) DO UPDATE 
SET full_name = EXCLUDED.full_name,
    phone = EXCLUDED.phone,
    email = EXCLUDED.email,
    national_id = EXCLUDED.national_id,
    dob = EXCLUDED.dob,
    status = EXCLUDED.status,
    updated_at = now();

-- Resident 3: Child - Unit A101 - No account yet
INSERT INTO data.residents (id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440102'::uuid,
    'Nguyễn Văn C',
    '0912345680',
    'nguyenvanc@example.com',
    '001234567892',
    '2010-05-10',
    'ACTIVE',
    NULL, -- No account yet
    now(),
    now()
) ON CONFLICT (id) DO UPDATE 
SET full_name = EXCLUDED.full_name,
    phone = EXCLUDED.phone,
    email = EXCLUDED.email,
    national_id = EXCLUDED.national_id,
    dob = EXCLUDED.dob,
    status = EXCLUDED.status,
    updated_at = now();

-- Resident 4: Primary resident - Unit A102 - Will have account (user_id to be linked after iam-service runs)
INSERT INTO data.residents (id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440103'::uuid,
    'Trần Thị D',
    '0912345681',
    'tranthid@example.com',
    '001234567893',
    '1975-07-25',
    'ACTIVE',
    NULL, -- Will be linked to user: tranthid (550e8400-e29b-41d4-a716-446655440111) after iam-service migration V22 runs
    now(),
    now()
) ON CONFLICT (id) DO UPDATE 
SET full_name = EXCLUDED.full_name,
    phone = EXCLUDED.phone,
    email = EXCLUDED.email,
    national_id = EXCLUDED.national_id,
    dob = EXCLUDED.dob,
    status = EXCLUDED.status,
    updated_at = now();

-- Resident 5: Family member - Unit A102 - No account yet
INSERT INTO data.residents (id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440104'::uuid,
    'Trần Văn E',
    '0912345682',
    'tranvane@example.com',
    '001234567894',
    '1980-09-30',
    'ACTIVE',
    NULL, -- No account yet
    now(),
    now()
) ON CONFLICT (id) DO UPDATE 
SET full_name = EXCLUDED.full_name,
    phone = EXCLUDED.phone,
    email = EXCLUDED.email,
    national_id = EXCLUDED.national_id,
    dob = EXCLUDED.dob,
    status = EXCLUDED.status,
    updated_at = now();

-- Resident 6: Primary resident - Unit A201 - Will have account (user_id to be linked after iam-service runs)
INSERT INTO data.residents (id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440105'::uuid,
    'Lê Văn F',
    '0912345683',
    'levanf@example.com',
    '001234567895',
    '1988-11-12',
    'ACTIVE',
    NULL, -- Will be linked to user: levanf (550e8400-e29b-41d4-a716-446655440112) after iam-service migration V22 runs
    now(),
    now()
) ON CONFLICT (id) DO UPDATE 
SET full_name = EXCLUDED.full_name,
    phone = EXCLUDED.phone,
    email = EXCLUDED.email,
    national_id = EXCLUDED.national_id,
    dob = EXCLUDED.dob,
    status = EXCLUDED.status,
    updated_at = now();

-- Resident 7: Inactive resident - Unit A201 - No account
INSERT INTO data.residents (id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440106'::uuid,
    'Lê Thị G',
    '0912345684',
    'lethig@example.com',
    '001234567896',
    '1990-12-25',
    'INACTIVE',
    NULL, -- No account
    now(),
    now()
) ON CONFLICT (id) DO UPDATE 
SET full_name = EXCLUDED.full_name,
    phone = EXCLUDED.phone,
    email = EXCLUDED.email,
    national_id = EXCLUDED.national_id,
    dob = EXCLUDED.dob,
    status = EXCLUDED.status,
    updated_at = now();

-- Resident 8: Primary resident - Unit B101 - Will have account (user_id to be linked after iam-service runs)
INSERT INTO data.residents (id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440107'::uuid,
    'Phạm Văn H',
    '0912345685',
    'phamvanh@example.com',
    '001234567897',
    '1970-02-14',
    'ACTIVE',
    NULL, -- Will be linked to user: phamvanh (550e8400-e29b-41d4-a716-446655440113) after iam-service migration V22 runs
    now(),
    now()
) ON CONFLICT (id) DO UPDATE 
SET full_name = EXCLUDED.full_name,
    phone = EXCLUDED.phone,
    email = EXCLUDED.email,
    national_id = EXCLUDED.national_id,
    dob = EXCLUDED.dob,
    status = EXCLUDED.status,
    updated_at = now();

-- Resident 9: Family member - Unit B101 - No account yet
INSERT INTO data.residents (id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440108'::uuid,
    'Phạm Thị I',
    '0912345686',
    'phamthi@example.com',
    '001234567898',
    '1975-04-18',
    'ACTIVE',
    NULL, -- No account yet
    now(),
    now()
) ON CONFLICT (id) DO UPDATE 
SET full_name = EXCLUDED.full_name,
    phone = EXCLUDED.phone,
    email = EXCLUDED.email,
    national_id = EXCLUDED.national_id,
    dob = EXCLUDED.dob,
    status = EXCLUDED.status,
    updated_at = now();

-- Resident 10: Resident without phone (for testing edge cases)
INSERT INTO data.residents (id, full_name, phone, email, national_id, dob, status, user_id, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440109'::uuid,
    'Hoàng Văn J',
    NULL, -- No phone
    'hoangvanj@example.com',
    '001234567899',
    '1985-06-22',
    'ACTIVE',
    NULL, -- No account yet
    now(),
    now()
) ON CONFLICT (id) DO UPDATE 
SET full_name = EXCLUDED.full_name,
    phone = EXCLUDED.phone,
    email = EXCLUDED.email,
    national_id = EXCLUDED.national_id,
    dob = EXCLUDED.dob,
    status = EXCLUDED.status,
    updated_at = now();

