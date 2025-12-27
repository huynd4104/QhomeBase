-- Remove tenant-related tables and functions
-- This migration removes the tenant architecture that was previously used

-- Drop function first (might reference tables)
DROP FUNCTION IF EXISTS iam.get_user_permissions_in_tenant(UUID, UUID) CASCADE;

-- Drop tenant-related tables
DROP TABLE IF EXISTS iam.user_tenant_roles CASCADE;
DROP TABLE IF EXISTS iam.tenant_role_permissions CASCADE;
DROP TABLE IF EXISTS iam.user_tenant_grants CASCADE;
DROP TABLE IF EXISTS iam.user_tenant_denies CASCADE;



