INSERT INTO iam.roles (role, description, is_global) VALUES
('account', 'Account', true),
('admin', 'Administrator', true),
('tenant_owner', 'Tenant Owner', true),
('technician', 'Technician', true),
('supporter', 'Supporter', true)
ON CONFLICT (role) DO UPDATE SET
    description = EXCLUDED.description,
    is_global = EXCLUDED.is_global;









































