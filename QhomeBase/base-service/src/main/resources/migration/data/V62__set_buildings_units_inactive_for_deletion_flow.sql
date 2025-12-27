-- V59: Transition building and unit statuses to INACTIVE for deletion workflow
-- 1. Convert legacy MAINTENANCE values (introduced by V58) to INACTIVE
-- 2. Ensure any remaining DELETING values are also set to INACTIVE

UPDATE data.buildings
SET status = 'INACTIVE'
WHERE status IN ('MAINTENANCE', 'DELETING');

-- For consistency, mark related units as INACTIVE when their building is INACTIVE
UPDATE data.units u
SET status = 'INACTIVE'
FROM data.buildings b
WHERE u.building_id = b.id
  AND b.status = 'INACTIVE'
  AND u.status <> 'INACTIVE';





