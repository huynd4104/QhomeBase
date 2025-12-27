ALTER TABLE iam.roles DROP COLUMN IF EXISTS is_global;

ALTER TABLE iam.users 
ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT true,
ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT now(),
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT now();

CREATE TABLE IF NOT EXISTS iam.role_assignment_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES iam.users(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL,
    role_name TEXT NOT NULL,
    action TEXT NOT NULL CHECK (action IN ('ASSIGN', 'REMOVE')),
    performed_by TEXT NOT NULL,
    reason TEXT,
    performed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    metadata JSONB
);

CREATE INDEX IF NOT EXISTS ix_users_active ON iam.users(is_active);
CREATE INDEX IF NOT EXISTS ix_users_created_at ON iam.users(created_at);

CREATE INDEX IF NOT EXISTS ix_user_tenant_roles_user_tenant ON iam.user_tenant_roles(user_id, tenant_id);
CREATE INDEX IF NOT EXISTS ix_user_tenant_roles_tenant_role ON iam.user_tenant_roles(tenant_id, role);
CREATE INDEX IF NOT EXISTS ix_user_tenant_roles_granted_at ON iam.user_tenant_roles(granted_at);

CREATE INDEX IF NOT EXISTS ix_role_assignment_audit_user ON iam.role_assignment_audit(user_id);
CREATE INDEX IF NOT EXISTS ix_role_assignment_audit_tenant ON iam.role_assignment_audit(tenant_id);
CREATE INDEX IF NOT EXISTS ix_role_assignment_audit_performed_at ON iam.role_assignment_audit(performed_at);

INSERT INTO iam.permissions (code, description) VALUES 
('iam.employee.read', 'View employee information'),
('iam.employee.role.assign', 'Assign roles to employees'),
('iam.employee.role.remove', 'Remove roles from employees'),
('iam.employee.role.bulk_assign', 'Bulk assign roles to employees'),
('iam.employee.export', 'Export employee list'),
('iam.employee.import', 'Import employee list'),
('iam.permission.manage', 'Manage user permissions'),
('base.building.manage', 'Manage building information'),
('base.unit.manage', 'Manage units/apartments'),
('base.resident.manage', 'Manage residents'),
('base.resident.approve', 'Approve resident applications'),
('finance.fee.manage', 'Manage fees and billing')
ON CONFLICT (code) DO NOTHING;

INSERT INTO iam.roles (role, description) VALUES 
('staff', 'Building Staff - Basic building operations')
ON CONFLICT (role) DO NOTHING;

INSERT INTO iam.role_permissions (role, permission_code) VALUES 
('staff', 'iam.employee.read'),
('staff', 'iam.user.read'),
('staff', 'iam.role.read'),
('staff', 'iam.role.permission.read'),
('staff', 'iam.permission.read'),

('account', 'iam.employee.read'),
('account', 'finance.fee.manage'),

('technician', 'iam.user.read'),
('technician', 'iam.role.read'),
('technician', 'iam.role.permission.read'),
('technician', 'iam.permission.read'),

('supporter', 'iam.user.read'),
('supporter', 'iam.role.read'),
('supporter', 'iam.role.permission.read'),
('supporter', 'iam.permission.read')
ON CONFLICT (role, permission_code) DO NOTHING;

-- Test user insertions removed for new database clones

