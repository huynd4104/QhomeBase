ALTER TABLE data.household_member_requests
    ALTER COLUMN resident_id DROP NOT NULL;

ALTER TABLE data.household_member_requests
    ADD COLUMN IF NOT EXISTS resident_full_name TEXT NOT NULL,
    ADD COLUMN IF NOT EXISTS resident_phone TEXT,
    ADD COLUMN IF NOT EXISTS resident_email TEXT,
    ADD COLUMN IF NOT EXISTS resident_national_id TEXT,
    ADD COLUMN IF NOT EXISTS resident_dob DATE;

CREATE INDEX IF NOT EXISTS idx_hmr_national_id
    ON data.household_member_requests (resident_national_id)
    WHERE resident_national_id IS NOT NULL;





