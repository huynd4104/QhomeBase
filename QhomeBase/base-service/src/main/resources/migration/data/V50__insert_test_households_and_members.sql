-- V50: Insert test households and household members for account creation workflow testing
-- This script creates households and links residents to units via household_members

-- Household 1: Unit A101 - OWNER - Primary resident: Nguyễn Văn A
INSERT INTO data.households (id, unit_id, kind, primary_resident_id, start_date, end_date, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440200'::uuid,
    '550e8400-e29b-41d4-a716-446655440010'::uuid, -- Unit A101
    'OWNER',
    '550e8400-e29b-41d4-a716-446655440100'::uuid, -- Nguyễn Văn A (Primary)
    '2020-01-01',
    NULL,
    now(),
    now()
) ON CONFLICT (id) DO UPDATE 
SET unit_id = EXCLUDED.unit_id,
    kind = EXCLUDED.kind,
    primary_resident_id = EXCLUDED.primary_resident_id,
    start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    updated_at = now();

-- Household Member 1: Nguyễn Văn A - Primary
INSERT INTO data.household_members (id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440300'::uuid,
    '550e8400-e29b-41d4-a716-446655440200'::uuid, -- Household 1
    '550e8400-e29b-41d4-a716-446655440100'::uuid, -- Nguyễn Văn A
    'Chủ hộ',
    true,
    '2020-01-01',
    NULL,
    now(),
    now()
) ON CONFLICT (id) DO UPDATE 
SET household_id = EXCLUDED.household_id,
    resident_id = EXCLUDED.resident_id,
    relation = EXCLUDED.relation,
    is_primary = EXCLUDED.is_primary,
    joined_at = EXCLUDED.joined_at,
    left_at = EXCLUDED.left_at,
    updated_at = now();

-- Household Member 2: Nguyễn Thị B - Wife
INSERT INTO data.household_members (id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440301'::uuid,
    '550e8400-e29b-41d4-a716-446655440200'::uuid, -- Household 1
    '550e8400-e29b-41d4-a716-446655440101'::uuid, -- Nguyễn Thị B
    'Vợ',
    false,
    '2020-01-01',
    NULL,
    now(),
    now()
) ON CONFLICT (id) DO UPDATE 
SET household_id = EXCLUDED.household_id,
    resident_id = EXCLUDED.resident_id,
    relation = EXCLUDED.relation,
    is_primary = EXCLUDED.is_primary,
    joined_at = EXCLUDED.joined_at,
    left_at = EXCLUDED.left_at,
    updated_at = now();

-- Household Member 3: Nguyễn Văn C - Son
INSERT INTO data.household_members (id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440302'::uuid,
    '550e8400-e29b-41d4-a716-446655440200'::uuid, -- Household 1
    '550e8400-e29b-41d4-a716-446655440102'::uuid, -- Nguyễn Văn C
    'Con trai',
    false,
    '2020-01-01',
    NULL,
    now(),
    now()
) ON CONFLICT (id) DO UPDATE 
SET household_id = EXCLUDED.household_id,
    resident_id = EXCLUDED.resident_id,
    relation = EXCLUDED.relation,
    is_primary = EXCLUDED.is_primary,
    joined_at = EXCLUDED.joined_at,
    left_at = EXCLUDED.left_at,
    updated_at = now();

-- Household 2: Unit A102 - OWNER - Primary resident: Trần Thị D
INSERT INTO data.households (id, unit_id, kind, primary_resident_id, start_date, end_date, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440201'::uuid,
    '550e8400-e29b-41d4-a716-446655440011'::uuid, -- Unit A102
    'OWNER',
    '550e8400-e29b-41d4-a716-446655440103'::uuid, -- Trần Thị D (Primary)
    '2020-02-01',
    NULL,
    now(),
    now()
) ON CONFLICT (id) DO UPDATE 
SET unit_id = EXCLUDED.unit_id,
    kind = EXCLUDED.kind,
    primary_resident_id = EXCLUDED.primary_resident_id,
    start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    updated_at = now();

-- Household Member 4: Trần Thị D - Primary
INSERT INTO data.household_members (id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440303'::uuid,
    '550e8400-e29b-41d4-a716-446655440201'::uuid, -- Household 2
    '550e8400-e29b-41d4-a716-446655440103'::uuid, -- Trần Thị D
    'Chủ hộ',
    true,
    '2020-02-01',
    NULL,
    now(),
    now()
) ON CONFLICT (id) DO UPDATE 
SET household_id = EXCLUDED.household_id,
    resident_id = EXCLUDED.resident_id,
    relation = EXCLUDED.relation,
    is_primary = EXCLUDED.is_primary,
    joined_at = EXCLUDED.joined_at,
    left_at = EXCLUDED.left_at,
    updated_at = now();

-- Household Member 5: Trần Văn E - Husband
INSERT INTO data.household_members (id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440304'::uuid,
    '550e8400-e29b-41d4-a716-446655440201'::uuid, -- Household 2
    '550e8400-e29b-41d4-a716-446655440104'::uuid, -- Trần Văn E
    'Chồng',
    false,
    '2020-02-01',
    NULL,
    now(),
    now()
) ON CONFLICT (id) DO UPDATE 
SET household_id = EXCLUDED.household_id,
    resident_id = EXCLUDED.resident_id,
    relation = EXCLUDED.relation,
    is_primary = EXCLUDED.is_primary,
    joined_at = EXCLUDED.joined_at,
    left_at = EXCLUDED.left_at,
    updated_at = now();

-- Household 3: Unit A201 - OWNER - Primary resident: Lê Văn F
INSERT INTO data.households (id, unit_id, kind, primary_resident_id, start_date, end_date, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440202'::uuid,
    '550e8400-e29b-41d4-a716-446655440012'::uuid, -- Unit A201
    'OWNER',
    '550e8400-e29b-41d4-a716-446655440105'::uuid, -- Lê Văn F (Primary)
    '2020-03-01',
    NULL,
    now(),
    now()
) ON CONFLICT (id) DO UPDATE 
SET unit_id = EXCLUDED.unit_id,
    kind = EXCLUDED.kind,
    primary_resident_id = EXCLUDED.primary_resident_id,
    start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    updated_at = now();

-- Household Member 6: Lê Văn F - Primary
INSERT INTO data.household_members (id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440305'::uuid,
    '550e8400-e29b-41d4-a716-446655440202'::uuid, -- Household 3
    '550e8400-e29b-41d4-a716-446655440105'::uuid, -- Lê Văn F
    'Chủ hộ',
    true,
    '2020-03-01',
    NULL,
    now(),
    now()
) ON CONFLICT (id) DO UPDATE 
SET household_id = EXCLUDED.household_id,
    resident_id = EXCLUDED.resident_id,
    relation = EXCLUDED.relation,
    is_primary = EXCLUDED.is_primary,
    joined_at = EXCLUDED.joined_at,
    left_at = EXCLUDED.left_at,
    updated_at = now();

-- Household Member 7: Lê Thị G - Wife (INACTIVE)
INSERT INTO data.household_members (id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440306'::uuid,
    '550e8400-e29b-41d4-a716-446655440202'::uuid, -- Household 3
    '550e8400-e29b-41d4-a716-446655440106'::uuid, -- Lê Thị G
    'Vợ',
    false,
    '2020-03-01',
    NULL,
    now(),
    now()
) ON CONFLICT (id) DO UPDATE 
SET household_id = EXCLUDED.household_id,
    resident_id = EXCLUDED.resident_id,
    relation = EXCLUDED.relation,
    is_primary = EXCLUDED.is_primary,
    joined_at = EXCLUDED.joined_at,
    left_at = EXCLUDED.left_at,
    updated_at = now();

-- Household 4: Unit B101 - OWNER - Primary resident: Phạm Văn H
INSERT INTO data.households (id, unit_id, kind, primary_resident_id, start_date, end_date, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440203'::uuid,
    '550e8400-e29b-41d4-a716-446655440020'::uuid, -- Unit B101
    'OWNER',
    '550e8400-e29b-41d4-a716-446655440107'::uuid, -- Phạm Văn H (Primary)
    '2020-04-01',
    NULL,
    now(),
    now()
) ON CONFLICT (id) DO UPDATE 
SET unit_id = EXCLUDED.unit_id,
    kind = EXCLUDED.kind,
    primary_resident_id = EXCLUDED.primary_resident_id,
    start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    updated_at = now();

-- Household Member 8: Phạm Văn H - Primary
INSERT INTO data.household_members (id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440307'::uuid,
    '550e8400-e29b-41d4-a716-446655440203'::uuid, -- Household 4
    '550e8400-e29b-41d4-a716-446655440107'::uuid, -- Phạm Văn H
    'Chủ hộ',
    true,
    '2020-04-01',
    NULL,
    now(),
    now()
) ON CONFLICT (id) DO UPDATE 
SET household_id = EXCLUDED.household_id,
    resident_id = EXCLUDED.resident_id,
    relation = EXCLUDED.relation,
    is_primary = EXCLUDED.is_primary,
    joined_at = EXCLUDED.joined_at,
    left_at = EXCLUDED.left_at,
    updated_at = now();

-- Household Member 9: Phạm Thị I - Wife
INSERT INTO data.household_members (id, household_id, resident_id, relation, is_primary, joined_at, left_at, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440308'::uuid,
    '550e8400-e29b-41d4-a716-446655440203'::uuid, -- Household 4
    '550e8400-e29b-41d4-a716-446655440108'::uuid, -- Phạm Thị I
    'Vợ',
    false,
    '2020-04-01',
    NULL,
    now(),
    now()
) ON CONFLICT (id) DO UPDATE 
SET household_id = EXCLUDED.household_id,
    resident_id = EXCLUDED.resident_id,
    relation = EXCLUDED.relation,
    is_primary = EXCLUDED.is_primary,
    joined_at = EXCLUDED.joined_at,
    left_at = EXCLUDED.left_at,
    updated_at = now();



