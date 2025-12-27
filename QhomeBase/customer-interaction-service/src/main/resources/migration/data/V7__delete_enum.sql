ALTER TABLE cs_service.requests
    ALTER COLUMN status TYPE VARCHAR(255) USING status::text::varchar,
    ALTER COLUMN priority TYPE VARCHAR(255) USING priority::text::varchar;

ALTER TABLE cs_service.processing_logs
    ALTER COLUMN record_type TYPE VARCHAR(255) USING record_type::text::varchar;

ALTER TABLE cs_service.complaints
    ALTER COLUMN priority TYPE VARCHAR(255) USING priority::text::varchar;

DROP TYPE cs_service.request_status;
DROP TYPE cs_service.priority_level;
DROP TYPE cs_service.record_type;