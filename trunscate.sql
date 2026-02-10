-- 1. Tắt kiểm tra ràng buộc để tránh lỗi khóa ngoại
SET session_replication_role = 'replica';

-- 2. Xóa sạch dữ liệu trong tất cả các bảng (giữ nguyên cấu trúc)
TRUNCATE TABLE 
    -- Schema asset
    asset.asset_suppliers, asset.assets, asset.maintenance_records, asset.maintenance_schedules, 
    asset.service, asset.service_availability, asset.service_booking, asset.service_booking_item, 
    asset.service_booking_slot, asset.service_category, asset.service_combo, asset.service_combo_item, 
    asset.service_kpi_metric, asset.service_kpi_target, asset.service_kpi_value, asset.service_option, 
    asset.service_ticket, asset.suppliers,

    -- Schema billing
    billing.billing_cycles, billing.external_links, billing.invoice_adjustments, billing.invoice_lines, 
    billing.invoices, billing.late_payment_charges, billing.late_payment_config, billing.payment_reminders, 
    billing.pricing_tiers, billing.service_pricing,

    -- Schema card
    card.card_fee_reminder_state, card.card_pricing, card.elevator_card_registration, 
    card.register_vehicle, card.register_vehicle_image, card.resident_card_registration,

    -- Schema chat_service
    chat_service.blocks, chat_service.conversation_participants, chat_service.conversations, 
    chat_service.direct_chat_files, chat_service.direct_invitations, chat_service.direct_message_deletions, 
    chat_service.direct_messages, chat_service.friendships, chat_service.group_files, 
    chat_service.group_invitations, chat_service.group_members, chat_service.groups, chat_service.messages,

    -- Schema content
    content.news, content.news_images, content.news_views, content.notification_device_token, 
    content.notification_views, content.notifications,

    -- Schema cs_service
    cs_service.complaints, cs_service.processing_logs, cs_service.requests,

    -- Schema data
    data.account_creation_requests, data.asset_inspection_items, data.asset_inspections, data.assets, 
    data.building_deletion_requests, data.buildings, data.cleaning_requests, 
    data.common_area_maintenance_requests, data.household_member_requests, data.household_members, 
    data.households, data.maintenance_requests, data.meter_reading_assignments, 
    data.meter_reading_reminders, data.meter_readings, data.meters, data.project_info, 
    data.reading_cycles, data.residents, data.services, data.unit_parties, data.units, 
    data.vehicle_registration_requests, data.vehicles,

    -- Schema files
    files.contract_files, files.contracts, files.file_metadata, files.video_storage,

    -- Schema finance
    finance.account_balances, finance.cash_accounts, finance.gateway_payment_links, 
    finance.gateway_transactions, finance.gateway_webhook_events, finance.idempotency_keys, 
    finance.ledger_entries, finance.payment_allocations, finance.payment_attempts, 
    finance.payment_gateways, finance.payment_intent_targets, finance.payment_intents, 
    finance.payment_reconciliation, finance.payments, finance.reconciliation_items, finance.refunds,

    -- Schema iam
    iam.auth_events, iam.jwks_keys, iam.permissions, iam.refresh_tokens, 
    iam.role_assignment_audit, iam.role_permissions, iam.roles, iam.user_roles, iam.users,

    -- Schema marketplace
    marketplace.marketplace_categories, marketplace.marketplace_comments, 
    marketplace.marketplace_likes, marketplace.marketplace_post_images, marketplace.marketplace_posts,

    -- Schema svc
    svc.card_assignments, svc.card_events, svc.card_packages, svc.cards, 
    svc.pricing_formulas, svc.pricing_tiers, svc.services
RESTART IDENTITY CASCADE;

-- 3. Bật lại kiểm tra ràng buộc
SET session_replication_role = 'origin';