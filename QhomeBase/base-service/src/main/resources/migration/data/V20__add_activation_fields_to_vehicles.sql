

ALTER TABLE data.vehicles
ADD COLUMN IF NOT EXISTS activated_at TIMESTAMPTZ,
ADD COLUMN IF NOT EXISTS registration_approved_at TIMESTAMPTZ,
ADD COLUMN IF NOT EXISTS approved_by UUID;

CREATE INDEX IF NOT EXISTS idx_vehicles_activated_at 
    ON data.vehicles(activated_at) 
    WHERE activated_at IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_vehicles_active_activated 
    ON data.vehicles(active, activated_at)
    WHERE active = true;

CREATE INDEX IF NOT EXISTS idx_vehicles_tenant_activated 
    ON data.vehicles(tenant_id, activated_at)
    WHERE active = true AND activated_at IS NOT NULL;

COMMENT ON COLUMN data.vehicles.activated_at IS 'Thời điểm xe được kích hoạt (bắt đầu tính tiền phí gửi xe)';
COMMENT ON COLUMN data.vehicles.registration_approved_at IS 'Thời điểm đơn đăng ký xe được phê duyệt';
COMMENT ON COLUMN data.vehicles.approved_by IS 'UUID của Manager đã phê duyệt đơn đăng ký xe';

UPDATE data.vehicles v
SET 
    activated_at = vrr.approved_at,
    registration_approved_at = vrr.approved_at,
    approved_by = vrr.approved_by
FROM data.vehicle_registration_requests vrr
WHERE vrr.vehicle_id = v.id
  AND vrr.status = 'APPROVED'
  AND v.activated_at IS NULL;

DO $$
DECLARE
    updated_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO updated_count
    FROM data.vehicles
    WHERE activated_at IS NOT NULL;
    
    RAISE NOTICE '✅ Migration completed. % vehicles have activation data.', updated_count;
END $$;




