-- Add composite index for household queries by unitId and endDate
-- This optimizes the query: WHERE unitId = ? AND (endDate IS NULL OR endDate >= CURRENT_DATE)
-- Composite index on (unit_id, end_date) allows PostgreSQL to efficiently filter by both columns
-- Note: Cannot use partial index with CURRENT_DATE (dynamic), so use full composite index
CREATE INDEX IF NOT EXISTS idx_households_unit_end_date 
ON data.households (unit_id, end_date NULLS FIRST);

-- Add index on start_date for ORDER BY startDate DESC optimization
-- This speeds up sorting when querying households by start date
CREATE INDEX IF NOT EXISTS idx_households_start_date_desc 
ON data.households (start_date DESC);

-- Add index for primary resident lookups (filtered index for non-null values)
-- This speeds up queries filtering by primary_resident_id
CREATE INDEX IF NOT EXISTS idx_households_primary_resident 
ON data.households (primary_resident_id) 
WHERE primary_resident_id IS NOT NULL;
