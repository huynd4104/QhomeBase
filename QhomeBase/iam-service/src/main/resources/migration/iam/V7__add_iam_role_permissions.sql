-- Add IAM Service permissions to existing roles

-- Admin role gets all IAM permissions
INSERT INTO iam.role_permissions (role, permission_code) VALUES
('admin', 'iam.user.role.manage'),
('admin', 'iam.user.permission.read'),
('admin', 'iam.user.password.reset'),
('admin', 'iam.user.password.change'),
('admin', 'iam.user.account.lock'),
('admin', 'iam.user.account.unlock'),
('admin', 'iam.role.remove'),
('admin', 'iam.role.permission.read'),
('admin', 'iam.role.permission.manage'),
('admin', 'iam.tenant.role.read'),
('admin', 'iam.tenant.role.assign'),
('admin', 'iam.tenant.role.remove'),
('admin', 'iam.tenant.manager.read'),
('admin', 'iam.system.stats.read'),
('admin', 'iam.system.settings.manage'),
('admin', 'iam.system.audit.read'),
('admin', 'iam.system.data.export'),
('admin', 'iam.system.data.import'),
('admin', 'iam.test.generate_token'),
('admin', 'iam.test.access');

-- Tenant Owner role gets user and tenant management permissions
INSERT INTO iam.role_permissions (role, permission_code) VALUES
('tenant_owner', 'iam.user.read'),
('tenant_owner', 'iam.user.update'),
('tenant_owner', 'iam.user.role.manage'),
('tenant_owner', 'iam.user.password.reset'),
('tenant_owner', 'iam.user.account.lock'),
('tenant_owner', 'iam.user.account.unlock'),
('tenant_owner', 'iam.role.read'),
('tenant_owner', 'iam.role.assign'),
('tenant_owner', 'iam.role.remove'),
('tenant_owner', 'iam.role.permission.read'),
('tenant_owner', 'iam.permission.read'),
('tenant_owner', 'iam.tenant.role.read'),
('tenant_owner', 'iam.tenant.role.create'),
('tenant_owner', 'iam.tenant.role.update'),
('tenant_owner', 'iam.tenant.role.delete'),
('tenant_owner', 'iam.tenant.role.assign'),
('tenant_owner', 'iam.tenant.role.remove'),
('tenant_owner', 'iam.tenant.manager.read'),
('tenant_owner', 'iam.system.audit.read'),
('tenant_owner', 'iam.system.data.export');

-- Technician role gets read-only permissions
INSERT INTO iam.role_permissions (role, permission_code) VALUES
('technician', 'iam.user.read'),
('technician', 'iam.role.read'),
('technician', 'iam.role.permission.read'),
('technician', 'iam.permission.read');

-- Supporter role gets read-only permissions
INSERT INTO iam.role_permissions (role, permission_code) VALUES
('supporter', 'iam.user.read'),
('supporter', 'iam.role.read'),
('supporter', 'iam.role.permission.read'),
('supporter', 'iam.permission.read');

-- Account role gets minimal permissions
INSERT INTO iam.role_permissions (role, permission_code) VALUES
('account', 'iam.user.password.change')

ON CONFLICT (role, permission_code) DO NOTHING;
