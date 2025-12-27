-- Add reissued_from_card_id column to register_vehicle table
-- This column tracks the original card ID when a card is reissued (REPLACE_CARD request)

ALTER TABLE card.register_vehicle
ADD COLUMN IF NOT EXISTS reissued_from_card_id UUID NULL;

-- Add comment to explain the column
COMMENT ON COLUMN card.register_vehicle.reissued_from_card_id IS 'ID của thẻ gốc nếu thẻ này là thẻ cấp lại (reissue). NULL nếu đây là thẻ mới (NEW_CARD)';

-- Add foreign key constraint for data integrity
-- Note: This creates a self-referencing foreign key
-- Only add constraint if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_register_vehicle_reissued_from'
    ) THEN
        ALTER TABLE card.register_vehicle
        ADD CONSTRAINT fk_register_vehicle_reissued_from
        FOREIGN KEY (reissued_from_card_id)
        REFERENCES card.register_vehicle(id)
        ON DELETE SET NULL;
    END IF;
END $$;

-- Create index for efficient querying of reissued cards
CREATE INDEX IF NOT EXISTS idx_vehicle_card_reissued_from
ON card.register_vehicle(reissued_from_card_id)
WHERE reissued_from_card_id IS NOT NULL;

