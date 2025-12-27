CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE SCHEMA IF NOT EXISTS cust;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_type t
    JOIN pg_namespace n ON n.oid = t.typnamespace
    WHERE t.typname = 'ticket_status' AND n.nspname = 'cust'
  ) THEN
CREATE TYPE cust.ticket_status AS ENUM ('NEW','ASSIGN','IN_PROGRESS','RESOLVED','CLOSED');
END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM pg_type t
    JOIN pg_namespace n ON n.oid = t.typnamespace
    WHERE t.typname = 'priority' AND n.nspname = 'cust'
  ) THEN
CREATE TYPE cust.priority AS ENUM ('LOW','MEDIUM','HIGH','URGENT');
END IF;
END $$;

CREATE TABLE IF NOT EXISTS cust.tickets (
                                            id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id              UUID NOT NULL,
    code                   TEXT NOT NULL,
    created_by_resident_id UUID,
    unit_id                UUID,
    category               TEXT,
    priority               cust.priority NOT NULL DEFAULT 'MEDIUM'::cust.priority,
    status                 cust.ticket_status NOT NULL DEFAULT 'NEW'::cust.ticket_status,
    subject                TEXT NOT NULL,
    description            TEXT,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, code)
    );

CREATE TABLE IF NOT EXISTS cust.ticket_assignees (
                                                     id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL,
    ticket_id     UUID NOT NULL REFERENCES cust.tickets(id) ON DELETE CASCADE,
    staff_user_id UUID NOT NULL,
    assigned_at   TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS cust.ticket_comments (
                                                    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL,
    ticket_id   UUID NOT NULL REFERENCES cust.tickets(id) ON DELETE CASCADE,
    author_type TEXT NOT NULL CHECK (author_type IN ('RESIDENT','STAFF')),
    author_id   UUID NOT NULL,
    content     TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS cust.ticket_photos (
                                                  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    ticket_id       UUID NOT NULL REFERENCES cust.tickets(id) ON DELETE CASCADE,
    storage_account TEXT NOT NULL,
    container       TEXT NOT NULL,
    blob_name       TEXT NOT NULL,
    filename        TEXT NOT NULL,
    mime_type       TEXT NOT NULL CHECK (mime_type LIKE 'image/%'),
    size_bytes      BIGINT NOT NULL CHECK (size_bytes >= 0),
    created_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (ticket_id, blob_name),
    CONSTRAINT ck_ticket_photos_tenant CHECK (tenant_id IS NOT NULL)
    );

CREATE INDEX IF NOT EXISTS idx_cust_tickets_tenant_status
    ON cust.tickets (tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_cust_tickets_tenant_created
    ON cust.tickets (tenant_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_cust_assignees_ticket
    ON cust.ticket_assignees (ticket_id, assigned_at DESC);

CREATE INDEX IF NOT EXISTS idx_cust_comments_ticket
    ON cust.ticket_comments (ticket_id, created_at ASC);

CREATE INDEX IF NOT EXISTS idx_cust_photos_ticket
    ON cust.ticket_photos (ticket_id, created_at DESC);
