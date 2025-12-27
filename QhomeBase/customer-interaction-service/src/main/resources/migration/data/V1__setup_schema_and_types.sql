CREATE SCHEMA IF NOT EXISTS cs_service;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid=t.typnamespace
        WHERE t.typname = 'request_status' AND n.nspname = 'cs_service'
    ) THEN
CREATE TYPE cs_service.request_status AS ENUM ('New', 'Processing', 'Completed', 'Closed');
END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid=t.typnamespace
        WHERE t.typname = 'complaint_status' AND n.nspname = 'cs_service'
    ) THEN
CREATE TYPE cs_service.complaint_status AS ENUM ('New', 'Processing', 'Investigating', 'Resolved', 'Closed');
END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid=t.typnamespace
        WHERE t.typname = 'priority_level' AND n.nspname = 'cs_service'
    ) THEN
CREATE TYPE cs_service.priority_level AS ENUM ('Low', 'Medium', 'High');
END IF;
END$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid=t.typnamespace
        WHERE t.typname = 'record_type' AND n.nspname = 'cs_service'
    ) THEN
CREATE TYPE cs_service.record_type AS ENUM ('Request', 'Complaint');
END IF;
END$$;
