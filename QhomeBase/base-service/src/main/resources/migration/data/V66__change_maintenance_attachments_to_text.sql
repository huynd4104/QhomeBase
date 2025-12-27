ALTER TABLE data.maintenance_requests
    ALTER COLUMN attachments TYPE TEXT USING attachments::text;


