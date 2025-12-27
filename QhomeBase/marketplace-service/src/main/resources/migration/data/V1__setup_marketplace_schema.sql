-- Create schema
CREATE SCHEMA IF NOT EXISTS marketplace;
CREATE SCHEMA IF NOT EXISTS mp_service;

-- Create enum type for post status
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'post_status') THEN
    CREATE TYPE marketplace.post_status AS ENUM ('ACTIVE', 'SOLD', 'DELETED');
  END IF;
END$$;

