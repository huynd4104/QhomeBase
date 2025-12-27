-- Add indexes to optimize notification queries for residents
-- These indexes significantly improve performance of getNotificationsForResidentPaged queries

-- Composite index for scope + deletedAt + createdAt (most common filter)
CREATE INDEX IF NOT EXISTS ix_notifications_scope_deleted_created 
ON content.notifications (scope, deleted_at, created_at DESC) 
WHERE deleted_at IS NULL;

-- Composite index for scope + targetResidentId + deletedAt (for resident-specific notifications)
CREATE INDEX IF NOT EXISTS ix_notifications_scope_resident_deleted 
ON content.notifications (scope, target_resident_id, deleted_at) 
WHERE scope = 'EXTERNAL' AND deleted_at IS NULL AND target_resident_id IS NOT NULL;

-- Composite index for scope + targetBuildingId + deletedAt (for building-specific notifications)
CREATE INDEX IF NOT EXISTS ix_notifications_scope_building_deleted 
ON content.notifications (scope, target_building_id, deleted_at, created_at DESC) 
WHERE scope = 'EXTERNAL' AND deleted_at IS NULL;

-- Composite index for type + targetResidentId (for card notifications)
CREATE INDEX IF NOT EXISTS ix_notifications_type_resident 
ON content.notifications (type, target_resident_id, deleted_at, created_at DESC) 
WHERE type IN ('CARD_FEE_REMINDER', 'CARD_APPROVED', 'CARD_REJECTED') 
AND deleted_at IS NULL;

COMMENT ON INDEX content.ix_notifications_scope_deleted_created IS 'Optimizes queries filtering by scope, deletedAt, and createdAt';
COMMENT ON INDEX content.ix_notifications_scope_resident_deleted IS 'Optimizes queries for resident-specific notifications';
COMMENT ON INDEX content.ix_notifications_scope_building_deleted IS 'Optimizes queries for building-specific notifications';
COMMENT ON INDEX content.ix_notifications_type_resident IS 'Optimizes queries for card-related notifications';
