-- Add new IAM Service specific permissions
INSERT INTO iam.permissions (code, description) VALUES
-- User Management permissions
('iam.user.role.manage', 'Manage user roles'),
('iam.user.permission.read', 'Read user permissions'),
('iam.user.password.reset', 'Reset user passwords'),
('iam.user.password.change', 'Change user passwords'),
('iam.user.account.lock', 'Lock user accounts'),
('iam.user.account.unlock', 'Unlock user accounts'),

-- Role Management permissions
('iam.role.remove', 'Remove roles from users'),
('iam.role.permission.read', 'Read role permissions'),
('iam.role.permission.manage', 'Manage role permissions'),

-- Tenant Role Management permissions
('iam.tenant.role.read', 'Read tenant roles'),
('iam.tenant.role.create', 'Create tenant roles'),
('iam.tenant.role.update', 'Update tenant roles'),
('iam.tenant.role.delete', 'Delete tenant roles'),
('iam.tenant.role.assign', 'Assign tenant roles'),
('iam.tenant.role.remove', 'Remove tenant roles'),
('iam.tenant.manager.read', 'Read tenant managers'),

-- System Admin permissions
('iam.system.stats.read', 'Read system statistics'),
('iam.system.settings.manage', 'Manage system settings'),
('iam.system.audit.read', 'Read audit logs'),
('iam.system.data.export', 'Export system data'),
('iam.system.data.import', 'Import system data'),

-- Testing permissions
('iam.test.generate_token', 'Generate test tokens'),
('iam.test.access', 'Access test endpoints')

ON CONFLICT (code) DO NOTHING;
