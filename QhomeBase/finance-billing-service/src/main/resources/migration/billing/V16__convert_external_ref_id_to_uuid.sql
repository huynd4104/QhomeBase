DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'billing'
          AND table_name = 'invoice_lines'
          AND column_name = 'external_ref_id'
          AND data_type <> 'uuid'
    ) THEN
        UPDATE billing.invoice_lines
        SET external_ref_id = NULL
        WHERE external_ref_id = '';

        ALTER TABLE billing.invoice_lines
            ALTER COLUMN external_ref_id TYPE uuid USING NULLIF(external_ref_id, '')::uuid;
    END IF;
END $$;

