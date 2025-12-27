ALTER TABLE asset.maintenance_records
    ADD COLUMN IF NOT EXISTS due_date DATE;


