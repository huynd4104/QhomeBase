ALTER TABLE asset.service_combo_item
    DROP CONSTRAINT IF EXISTS fk_service_combo_item_included_service;

ALTER TABLE asset.service_combo_item
    DROP CONSTRAINT IF EXISTS fk_service_combo_item_option;

ALTER TABLE asset.service_combo_item
    DROP COLUMN IF EXISTS included_service_id,
    DROP COLUMN IF EXISTS option_id;

ALTER TABLE asset.service_combo_item
    ADD COLUMN IF NOT EXISTS item_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS item_description TEXT,
    ADD COLUMN IF NOT EXISTS item_price NUMERIC(15, 2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS item_duration_minutes INTEGER;

UPDATE asset.service_combo_item
SET item_name = COALESCE(item_name, 'Unnamed item')
WHERE item_name IS NULL;

ALTER TABLE asset.service_combo_item
    ALTER COLUMN item_name SET NOT NULL,
    ALTER COLUMN item_price SET NOT NULL;

