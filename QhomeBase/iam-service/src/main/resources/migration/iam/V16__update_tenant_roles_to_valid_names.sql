
UPDATE iam.user_tenant_roles SET role = 'admin' WHERE role = 'tenant_admin';
UPDATE iam.user_tenant_roles SET role = 'tenant_owner' WHERE role = 'tenant_manager';
UPDATE iam.user_tenant_roles SET role = 'technician' WHERE role = 'tenant_tech';
UPDATE iam.user_tenant_roles SET role = 'supporter' WHERE role = 'tenant_support';
UPDATE iam.user_tenant_roles SET role = 'account' WHERE role = 'tenant_account';


UPDATE iam.tenant_role_permissions SET role = 'admin' WHERE role = 'tenant_admin';
UPDATE iam.tenant_role_permissions SET role = 'tenant_owner' WHERE role = 'tenant_manager';
UPDATE iam.tenant_role_permissions SET role = 'technician' WHERE role = 'tenant_tech';
UPDATE iam.tenant_role_permissions SET role = 'supporter' WHERE role = 'tenant_support';
UPDATE iam.tenant_role_permissions SET role = 'account' WHERE role = 'tenant_account';


DELETE FROM iam.user_tenant_roles WHERE role NOT IN ('admin', 'tenant_owner', 'technician', 'supporter', 'account');
DELETE FROM iam.tenant_role_permissions WHERE role NOT IN ('admin', 'tenant_owner', 'technician', 'supporter', 'account');
