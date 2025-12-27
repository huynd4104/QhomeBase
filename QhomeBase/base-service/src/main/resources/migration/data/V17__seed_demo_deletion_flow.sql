
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
DO $$
DECLARE
  t1 UUID := gen_random_uuid();
  t2 UUID := gen_random_uuid();
  b1 UUID := gen_random_uuid();
  b2 UUID := gen_random_uuid();
  u1 UUID := gen_random_uuid();
  u2 UUID := gen_random_uuid();
  u3 UUID := gen_random_uuid();
BEGIN

  INSERT INTO data.tenants(id, code, name, contact, email, status, created_at, updated_at)
  VALUES
    (t1, 'T_DEMO_ACTIVE', 'Tenant Demo Active', '0123456789', 'demo1@example.com', 'ACTIVE', now(), now()),
    (t2, 'T_DEMO_EMPTY',  'Tenant Demo Empty',  '0987654321', 'demo2@example.com', 'ACTIVE', now(), now())
  ON CONFLICT (code) DO NOTHING;

  INSERT INTO data."buildings"(id, tenant_id, code, name, address, created_at, updated_at)
  VALUES
    (b1, t1, 'BLD-A', 'Building A', 'A Street', now(), now()),
    (b2, t1, 'BLD-B', 'Building B', 'B Street', now(), now())
  ON CONFLICT DO NOTHING;

  -- Ensure building status columns exist and set statuses
  UPDATE data."buildings" SET status = 'ACTIVE', is_deleted = FALSE WHERE id = b1;
  UPDATE data."buildings" SET status = 'PENDING_DELETION', is_deleted = FALSE WHERE id = b2;

  -- Units for buildings
  INSERT INTO data.units(id, tenant_id, building_id, code, floor, area_m2, bedrooms, status, created_at, updated_at)
  VALUES
    (u1, t1, b1, 'A-01', 1, 50.0, 2, 'ACTIVE', now(), now()),
    (u2, t1, b1, 'A-02', 2, 60.0, 3, 'ACTIVE', now(), now()),
    (u3, t1, b2, 'B-01', 1, 55.0, 2, 'INACTIVE', now(), now())
  ON CONFLICT DO NOTHING;

END$$;


















































