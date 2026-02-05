-- 1. INSERT ROLES (UPPERCASE as per V21)
INSERT INTO iam.roles (role, description) VALUES
                                              ('ADMIN', 'System Administrator'),
                                              ('ACCOUNTANT', 'Financial and Accounting Manager'),
                                              ('TECHNICIAN', 'Technical and Maintenance Staff'),
                                              ('SUPPORTER', 'Customer Support Specialist'),
                                              ('RESIDENT', 'Apartment Resident'),
                                              ('UNIT_OWNER', 'Apartment Unit Owner')
    ON CONFLICT (role) DO UPDATE SET description = EXCLUDED.description;

-- 2. INSERT PERMISSIONS (Consolidated from V3, V6, V10)
INSERT INTO iam.permissions (code, description) VALUES
-- Tenant & Building
('base.tenant.read', 'Read tenant information'),
('base.building.read', 'Read building information'),
('base.unit.view', 'View units'),
-- User Management
('iam.user.read', 'Read user information'),
('iam.user.update', 'Update user information'),
('iam.user.password.reset', 'Reset user passwords'),
('iam.user.password.change', 'Change user passwords'),
('iam.role.read', 'Read role information'),
-- Maintenance & Finance
('maintenance.request.create', 'Create maintenance requests'),
('maintenance.request.read', 'Read maintenance information'),
('finance.read', 'Read financial information'),
-- System
('system.log', 'View system logs')
    ON CONFLICT (code) DO NOTHING;

-- 3. ROLE-PERMISSION MAPPING (Example for ADMIN & RESIDENT)
-- Admin: Full access (Chỉ ví dụ một số quyền chính)
INSERT INTO iam.role_permissions (role, permission_code)
SELECT 'ADMIN', code FROM iam.permissions
    ON CONFLICT DO NOTHING;

-- Resident: Basic access
INSERT INTO iam.role_permissions (role, permission_code) VALUES
                                                             ('RESIDENT', 'base.unit.view'),
                                                             ('RESIDENT', 'iam.user.password.change'),
                                                             ('RESIDENT', 'maintenance.request.create')
    ON CONFLICT DO NOTHING;