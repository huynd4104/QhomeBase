ALTER TABLE files.contracts ALTER COLUMN status DROP DEFAULT;
DELETE FROM files.contracts WHERE contract_number LIKE 'TEST-%';

WITH numbered_units AS (
    SELECT 
        u.id, u.code, u.area_m2, u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A' AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    monthly_rent, status, renewal_status, created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(), nu.id,
    'TEST-RENEWAL-' || nu.building_code || '-' || nu.code || '-PENDING',
    'RENTAL',
    CURRENT_DATE - INTERVAL '6 months',
    CURRENT_DATE + INTERVAL '15 days',
    CASE 
        WHEN nu.area_m2 < 60 THEN 3000000
        WHEN nu.area_m2 < 80 THEN 5000000
        WHEN nu.area_m2 < 100 THEN 7000000
        ELSE 10000000
    END,
    'ACTIVE', 'PENDING',
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(), NOW()
FROM numbered_units nu
WHERE nu.rn % 5 = 1;

WITH numbered_units AS (
    SELECT 
        u.id, u.code, u.area_m2, u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A' AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    monthly_rent, status, renewal_status, renewal_reminder_sent_at,
    created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(), nu.id,
    'TEST-RENEWAL-' || nu.building_code || '-' || nu.code || '-REMINDED-7DAYS',
    'RENTAL',
    CURRENT_DATE - INTERVAL '6 months',
    CURRENT_DATE + INTERVAL '19 days',
    CASE 
        WHEN nu.area_m2 < 60 THEN 3000000
        WHEN nu.area_m2 < 80 THEN 5000000
        WHEN nu.area_m2 < 100 THEN 7000000
        ELSE 10000000
    END,
    'ACTIVE', 'REMINDED', NOW() - INTERVAL '7 days',
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(), NOW()
FROM numbered_units nu
WHERE nu.rn % 5 = 2;

WITH numbered_units AS (
    SELECT 
        u.id, u.code, u.area_m2, u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A' AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    monthly_rent, status, renewal_status, renewal_reminder_sent_at,
    created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(), nu.id,
    'TEST-RENEWAL-' || nu.building_code || '-' || nu.code || '-REMINDED-20DAYS',
    'RENTAL',
    CURRENT_DATE - INTERVAL '1 year',
    CASE 
        WHEN EXTRACT(DAY FROM CURRENT_DATE) < 20 THEN 
            DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '19 days'
        ELSE 
            DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' + INTERVAL '19 days'
    END,
    CASE 
        WHEN nu.area_m2 < 60 THEN 3000000
        WHEN nu.area_m2 < 80 THEN 5000000
        WHEN nu.area_m2 < 100 THEN 7000000
        ELSE 10000000
    END,
    'ACTIVE', 'REMINDED', NOW() - INTERVAL '20 days',
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(), NOW()
FROM numbered_units nu
WHERE nu.rn % 5 = 3;

WITH numbered_units AS (
    SELECT 
        u.id, u.code, u.area_m2, u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A' AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    monthly_rent, status, renewal_status, renewal_reminder_sent_at,
    created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(), nu.id,
    'TEST-RENEWAL-' || nu.building_code || '-' || nu.code || '-REMINDED-21DAYS',
    'RENTAL',
    CURRENT_DATE - INTERVAL '1 year',
    CURRENT_DATE + INTERVAL '21 days',
    CASE 
        WHEN nu.area_m2 < 60 THEN 3000000
        WHEN nu.area_m2 < 80 THEN 5000000
        WHEN nu.area_m2 < 100 THEN 7000000
        ELSE 10000000
    END,
    'ACTIVE', 'REMINDED', NOW() - INTERVAL '21 days',
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(), NOW()
FROM numbered_units nu
WHERE nu.rn % 5 = 4;

WITH numbered_units AS (
    SELECT 
        u.id, u.code, u.area_m2, u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A' AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    monthly_rent, status, renewal_status, renewal_reminder_sent_at, renewal_declined_at,
    created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(), nu.id,
    'TEST-RENEWAL-' || nu.building_code || '-' || nu.code || '-DECLINED',
    'RENTAL',
    CURRENT_DATE - INTERVAL '1 year',
    CURRENT_DATE + INTERVAL '25 days',
    CASE 
        WHEN nu.area_m2 < 60 THEN 3000000
        WHEN nu.area_m2 < 80 THEN 5000000
        WHEN nu.area_m2 < 100 THEN 7000000
        ELSE 10000000
    END,
    'ACTIVE', 'DECLINED', NOW() - INTERVAL '25 days', NOW() - INTERVAL '5 days',
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(), NOW()
FROM numbered_units nu
WHERE nu.rn % 5 = 0;

WITH numbered_units AS (
    SELECT 
        u.id, u.code, u.area_m2, u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A' AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    monthly_rent, status, renewal_status, created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(), nu.id,
    'TEST-STATUS-' || nu.building_code || '-' || nu.code || '-INACTIVE-1DAY',
    'RENTAL',
    CURRENT_DATE + INTERVAL '1 day',
    CURRENT_DATE + INTERVAL '365 days',
    CASE 
        WHEN nu.area_m2 < 60 THEN 3000000
        WHEN nu.area_m2 < 80 THEN 5000000
        WHEN nu.area_m2 < 100 THEN 7000000
        ELSE 10000000
    END,
    'INACTIVE', 'PENDING',
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(), NOW()
FROM numbered_units nu
WHERE nu.rn % 11 = 1;

WITH numbered_units AS (
    SELECT 
        u.id, u.code, u.area_m2, u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A' AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    monthly_rent, status, renewal_status, created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(), nu.id,
    'TEST-STATUS-' || nu.building_code || '-' || nu.code || '-INACTIVE-7DAYS',
    'RENTAL',
    CURRENT_DATE + INTERVAL '7 days',
    CURRENT_DATE + INTERVAL '372 days',
    CASE 
        WHEN nu.area_m2 < 60 THEN 3000000
        WHEN nu.area_m2 < 80 THEN 5000000
        WHEN nu.area_m2 < 100 THEN 7000000
        ELSE 10000000
    END,
    'INACTIVE', 'PENDING',
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(), NOW()
FROM numbered_units nu
WHERE nu.rn % 11 = 2;

WITH numbered_units AS (
    SELECT 
        u.id, u.code, u.area_m2, u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A' AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    monthly_rent, status, renewal_status, created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(), nu.id,
    'TEST-STATUS-' || nu.building_code || '-' || nu.code || '-EXPIRED',
    'RENTAL',
    CURRENT_DATE - INTERVAL '365 days',
    CURRENT_DATE - INTERVAL '30 days',
    CASE 
        WHEN nu.area_m2 < 60 THEN 3000000
        WHEN nu.area_m2 < 80 THEN 5000000
        WHEN nu.area_m2 < 100 THEN 7000000
        ELSE 10000000
    END,
    'EXPIRED', 'DECLINED',
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(), NOW()
FROM numbered_units nu
WHERE nu.rn % 11 = 3;

WITH numbered_units AS (
    SELECT 
        u.id, u.code, u.area_m2, u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A' AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    checkout_date, monthly_rent, status, renewal_status,
    created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(), nu.id,
    'TEST-STATUS-' || nu.building_code || '-' || nu.code || '-CANCELLED',
    'RENTAL',
    CURRENT_DATE - INTERVAL '180 days',
    CURRENT_DATE + INTERVAL '90 days',
    CURRENT_DATE - INTERVAL '5 days',
    CASE 
        WHEN nu.area_m2 < 60 THEN 3000000
        WHEN nu.area_m2 < 80 THEN 5000000
        WHEN nu.area_m2 < 100 THEN 7000000
        ELSE 10000000
    END,
    'CANCELLED', 'DECLINED',
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(), NOW()
FROM numbered_units nu
WHERE nu.rn % 11 = 4;

WITH numbered_units AS (
    SELECT 
        u.id, u.code, u.area_m2, u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A' AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    monthly_rent, status, renewal_status, created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(), nu.id,
    'TEST-STATUS-' || nu.building_code || '-' || nu.code || '-NO-ENDDATE',
    'RENTAL',
    CURRENT_DATE - INTERVAL '100 days',
    NULL,
    CASE 
        WHEN nu.area_m2 < 60 THEN 3000000
        WHEN nu.area_m2 < 80 THEN 5000000
        WHEN nu.area_m2 < 100 THEN 7000000
        ELSE 10000000
    END,
    'ACTIVE', 'PENDING',
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(), NOW()
FROM numbered_units nu
WHERE nu.rn % 11 = 5;

WITH numbered_units AS (
    SELECT 
        u.id, u.code, u.area_m2, u.building_id,
        b.code as building_code,
        ROW_NUMBER() OVER (PARTITION BY u.building_id ORDER BY u.code) as rn
    FROM data.units u
    INNER JOIN data.buildings b ON u.building_id = b.id
    WHERE b.code != 'A' AND u.status = 'ACTIVE'
)
INSERT INTO files.contracts (
    id, unit_id, contract_number, contract_type, start_date, end_date,
    purchase_price, status, created_by, created_at, updated_at
)
SELECT
    gen_random_uuid(), nu.id,
    'TEST-STATUS-' || nu.building_code || '-' || nu.code || '-PURCHASE',
    'PURCHASE',
    CURRENT_DATE - INTERVAL '200 days',
    CURRENT_DATE - INTERVAL '10 days',
    CASE 
        WHEN nu.area_m2 < 60 THEN 2000000000
        WHEN nu.area_m2 < 80 THEN 3000000000
        WHEN nu.area_m2 < 100 THEN 4000000000
        ELSE 5000000000
    END,
    'ACTIVE',
    COALESCE(
        (SELECT id FROM iam.users WHERE username = 'admin test' LIMIT 1),
        '00000000-0000-0000-0000-000000000001'::uuid
    ),
    NOW(), NOW()
FROM numbered_units nu
WHERE nu.rn % 11 = 6;

SELECT 
    status,
    COUNT(*) as count
FROM files.contracts
WHERE contract_number LIKE 'TEST-%'
GROUP BY status
ORDER BY status;

SELECT 
    CASE 
        WHEN contract_number LIKE '%INACTIVE%' THEN 'Should be INACTIVE'
        WHEN contract_number LIKE '%EXPIRED%' THEN 'Should be EXPIRED'
        WHEN contract_number LIKE '%CANCELLED%' THEN 'Should be CANCELLED'
        ELSE 'Should be ACTIVE'
    END as expected_status,
    status as actual_status,
    COUNT(*)
FROM files.contracts
WHERE contract_number LIKE 'TEST-%'
GROUP BY expected_status, status
ORDER BY expected_status, status;
