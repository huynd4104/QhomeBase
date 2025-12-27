-- Add COMPLETED to building_deletion_status enum
DO $$
BEGIN
  -- Check if COMPLETED value already exists
  IF NOT EXISTS (
    SELECT 1
    FROM pg_enum
    WHERE enumlabel = 'COMPLETED'
      AND enumtypid = (
        SELECT oid
        FROM pg_type
        WHERE typname = 'building_deletion_status'
          AND typnamespace = (
            SELECT oid
            FROM pg_namespace
            WHERE nspname = 'data'
          )
      )
  ) THEN
    -- Add COMPLETED to the enum
    ALTER TYPE data.building_deletion_status ADD VALUE 'COMPLETED';
  END IF;
END$$;



