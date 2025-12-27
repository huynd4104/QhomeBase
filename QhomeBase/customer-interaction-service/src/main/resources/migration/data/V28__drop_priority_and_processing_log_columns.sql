ALTER TABLE cs_service.requests
    DROP COLUMN IF EXISTS priority;

ALTER TABLE cs_service.processing_logs
    DROP COLUMN IF EXISTS record_type;

ALTER TABLE cs_service.processing_logs
    DROP COLUMN IF EXISTS log_type;



