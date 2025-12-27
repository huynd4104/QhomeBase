-- Drop legacy columns if they still exist after previous migrations
ALTER TABLE cs_service.requests
    DROP COLUMN IF EXISTS priority;

ALTER TABLE cs_service.processing_logs
    DROP COLUMN IF EXISTS record_type;

ALTER TABLE cs_service.processing_logs
    DROP COLUMN IF EXISTS log_type;

-- Rename request_status enum values to match new workflow (Pending → Processing → Done)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_enum e
                 JOIN pg_type t ON e.enumtypid = t.oid
                 JOIN pg_namespace n ON t.typnamespace = n.oid
        WHERE n.nspname = 'cs_service'
          AND t.typname = 'request_status'
          AND e.enumlabel = 'New'
    ) THEN
        EXECUTE 'ALTER TYPE cs_service.request_status RENAME VALUE ''New'' TO ''Pending''';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_enum e
                 JOIN pg_type t ON e.enumtypid = t.oid
                 JOIN pg_namespace n ON t.typnamespace = n.oid
        WHERE n.nspname = 'cs_service'
          AND t.typname = 'request_status'
          AND e.enumlabel = 'Completed'
    ) THEN
        EXECUTE 'ALTER TYPE cs_service.request_status RENAME VALUE ''Completed'' TO ''Done''';
    END IF;
END
$$;


