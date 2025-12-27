INSERT INTO billing.service_pricing (tenant_id, service_code, service_name, category, base_price, unit, tax_rate, effective_from, description)
VALUES 
    ('00000000-0000-0000-0000-000000000000', 'PARKING_CAR', 'Phí gửi xe ô tô', 'PARKING', 500000.00, 'month', 0.00, '2024-01-01', 'Phí gửi xe ô tô hàng tháng'),
    ('00000000-0000-0000-0000-000000000000', 'PARKING_MOTORBIKE', 'Phí gửi xe máy', 'PARKING', 100000.00, 'month', 0.00, '2024-01-01', 'Phí gửi xe máy hàng tháng'),
    ('00000000-0000-0000-0000-000000000000', 'PARKING_BICYCLE', 'Phí gửi xe đạp', 'PARKING', 50000.00, 'month', 0.00, '2024-01-01', 'Phí gửi xe đạp hàng tháng'),
    ('00000000-0000-0000-0000-000000000000', 'WATER', 'Tiền nước', 'UTILITIES', 15000.00, 'm3', 5.00, '2024-01-01', 'Giá nước sinh hoạt'),
    ('00000000-0000-0000-0000-000000000000', 'ELECTRIC', 'Tiền điện', 'UTILITIES', 3000.00, 'kWh', 10.00, '2024-01-01', 'Giá điện sinh hoạt'),
    ('00000000-0000-0000-0000-000000000000', 'MAINTENANCE', 'Phí bảo trì', 'MAINTENANCE', 50000.00, 'month', 0.00, '2024-01-01', 'Phí bảo trì chung cư'),
    ('00000000-0000-0000-0000-000000000000', 'SECURITY', 'Phí bảo vệ', 'SERVICE', 30000.00, 'month', 0.00, '2024-01-01', 'Phí dịch vụ bảo vệ'),
    ('00000000-0000-0000-0000-000000000000', 'CLEANING', 'Phí vệ sinh', 'SERVICE', 20000.00, 'month', 0.00, '2024-01-01', 'Phí vệ sinh chung')
ON CONFLICT (tenant_id, service_code, effective_from) DO NOTHING;

INSERT INTO billing.late_payment_config (tenant_id, days_overdue, penalty_type, penalty_value, description)
VALUES 
    ('00000000-0000-0000-0000-000000000000', 7, 'PERCENTAGE', 1.00, 'Phạt 1% nếu trễ 1 tuần'),
    ('00000000-0000-0000-0000-000000000000', 15, 'PERCENTAGE', 2.00, 'Phạt 2% nếu trễ 15 ngày'),
    ('00000000-0000-0000-0000-000000000000', 30, 'PERCENTAGE', 5.00, 'Phạt 5% nếu trễ 1 tháng'),
    ('00000000-0000-0000-0000-000000000000', 60, 'FIXED_AMOUNT', 100000.00, 'Phạt cố định 100k nếu trễ 2 tháng')
ON CONFLICT (tenant_id, days_overdue) DO NOTHING;

COMMENT ON TABLE billing.service_pricing IS 'Sample data với tenant_id = 00000000-0000-0000-0000-000000000000 chỉ để tham khảo. Production cần thay bằng tenant thực tế.';




