ALTER TABLE cs_service.processing_logs
ADD COLUMN request_status VARCHAR(255);
CREATE INDEX IF NOT EXISTS idx_processing_logs_request_status ON cs_service.processing_logs (request_status);

ALTER TABLE cs_service.processing_logs
ADD COLUMN log_type VARCHAR(255);
CREATE INDEX IF NOT EXISTS idx_processing_logs_log_type ON cs_service.processing_logs (log_type);

ALTER TABLE cs_service.processing_logs
ADD COLUMN staff_in_charge_name VARCHAR(255);

ALTER TABLE cs_service.processing_logs
ADD COLUMN process_log_status VARCHAR(255);

ALTER TABLE cs_service.requests
ADD COLUMN request_code VARCHAR(15) UNIQUE;
