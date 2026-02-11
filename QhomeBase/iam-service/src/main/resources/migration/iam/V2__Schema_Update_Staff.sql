-- Create table iam.staff_profiles
CREATE TABLE IF NOT EXISTS iam.staff_profiles (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
    full_name text NOT NULL,
    phone character varying(20),
    national_id text, -- CCCD
    address text,
    CONSTRAINT uq_staff_user UNIQUE (user_id)
);

-- Remove phone from iam.users
ALTER TABLE iam.users DROP COLUMN IF EXISTS phone;
