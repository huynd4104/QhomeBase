-- V22: Drop paid_service_history table
-- Reverting V21 - using existing billing.invoices and billing.invoice_lines instead

DROP TABLE IF EXISTS billing.paid_service_history CASCADE;
