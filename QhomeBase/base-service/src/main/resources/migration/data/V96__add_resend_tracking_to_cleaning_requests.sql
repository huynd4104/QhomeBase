-- V96: Add resend tracking fields to cleaning_requests table
-- These fields are used to track reminder and resend functionality

ALTER TABLE data.cleaning_requests
    ADD COLUMN IF NOT EXISTS resend_alert_sent BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS last_resent_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_cleaning_requests_resend_alert 
    ON data.cleaning_requests(resend_alert_sent, created_at) 
    WHERE status = 'PENDING';

