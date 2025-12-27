-- V78: Add resend tracking fields to maintenance_requests table
-- These fields are used to track reminder and resend functionality

ALTER TABLE data.maintenance_requests
    ADD COLUMN IF NOT EXISTS resend_alert_sent BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS last_resent_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS call_alert_sent BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_maintenance_requests_resend_alert 
    ON data.maintenance_requests(resend_alert_sent, created_at) 
    WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_maintenance_requests_call_alert 
    ON data.maintenance_requests(call_alert_sent, created_at) 
    WHERE status = 'PENDING';

