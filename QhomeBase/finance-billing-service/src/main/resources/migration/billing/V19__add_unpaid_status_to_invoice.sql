-- Add UNPAID status to inv_status enum
DO $do$
BEGIN
  -- Check if UNPAID already exists in the enum
  IF NOT EXISTS (
    SELECT 1
    FROM pg_enum e
    JOIN pg_type t ON e.enumtypid = t.oid
    JOIN pg_namespace n ON t.typnamespace = n.oid
    WHERE t.typname = 'inv_status' 
    AND n.nspname = 'billing'
    AND e.enumlabel = 'UNPAID'
  ) THEN
    -- Add UNPAID to the enum
    ALTER TYPE billing.inv_status ADD VALUE 'UNPAID';
  END IF;
END
$do$;

COMMENT ON TYPE billing.inv_status IS 'Invoice status: DRAFT, PUBLISHED, PAID, VOID, UNPAID (chưa thanh toán sau khi nhắc đủ 3 lần + 1 lần cảnh báo cuối)';

