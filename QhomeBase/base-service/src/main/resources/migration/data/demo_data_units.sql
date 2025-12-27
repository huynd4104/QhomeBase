INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '1---01',
    1,
    50.00,
    1,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'A'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '2---02',
    2,
    65.50,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'A'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '3---03',
    3,
    80.00,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'A'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '4---04',
    4,
    95.75,
    3,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'A'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '5---05',
    5,
    75.25,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'A'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '1---01',
    1,
    55.00,
    1,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'B'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '2---02',
    2,
    70.50,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'B'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '3---03',
    3,
    85.00,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'B'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '4---04',
    4,
    100.00,
    3,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'B'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '5---05',
    5,
    78.50,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'B'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '1---01',
    1,
    60.00,
    1,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'C'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '2---02',
    2,
    75.50,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'C'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '3---03',
    3,
    90.00,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'C'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '4---04',
    4,
    105.25,
    3,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'C'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '5---05',
    5,
    82.75,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'C'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '1---01',
    1,
    48.00,
    1,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'D'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '2---02',
    2,
    63.50,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'D'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '3---03',
    3,
    78.00,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'D'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '4---04',
    4,
    92.50,
    3,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'D'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '5---05',
    5,
    73.25,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'D'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '1---01',
    1,
    52.00,
    1,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'E'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '2---02',
    2,
    67.50,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'E'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '3---03',
    3,
    82.00,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'E'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '4---04',
    4,
    97.75,
    3,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'E'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '5---05',
    5,
    76.50,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'E'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '1---01',
    1,
    54.00,
    1,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'F'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '2---02',
    2,
    69.50,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'F'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '3---03',
    3,
    84.00,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'F'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '4---04',
    4,
    99.25,
    3,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'F'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '5---05',
    5,
    77.75,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'F'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '1---01',
    1,
    56.00,
    1,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'G'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '2---02',
    2,
    71.50,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'G'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '3---03',
    3,
    86.00,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'G'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '4---04',
    4,
    101.00,
    3,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'G'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '5---05',
    5,
    79.50,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'G'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '1---01',
    1,
    58.00,
    1,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'H'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '2---02',
    2,
    73.50,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'H'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '3---03',
    3,
    88.00,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'H'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '4---04',
    4,
    103.25,
    3,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'H'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '5---05',
    5,
    81.75,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'H'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '1---01',
    1,
    60.00,
    1,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'I'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '2---02',
    2,
    75.50,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'I'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '3---03',
    3,
    90.00,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'I'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '4---04',
    4,
    105.50,
    3,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'I'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '5---05',
    5,
    83.25,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'I'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '1---01',
    1,
    62.00,
    1,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'J'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '2---02',
    2,
    77.50,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'J'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '3---03',
    3,
    92.00,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'J'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '4---04',
    4,
    107.75,
    3,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'J'
ON CONFLICT (building_id, code) DO NOTHING;

INSERT INTO data.units (
    id,
    building_id,
    code,
    floor,
    area_m2,
    bedrooms,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.code || '5---05',
    5,
    85.50,
    2,
    'ACTIVE',
    now(),
    now()
FROM data.buildings b
WHERE b.code = 'J'
ON CONFLICT (building_id, code) DO NOTHING;


