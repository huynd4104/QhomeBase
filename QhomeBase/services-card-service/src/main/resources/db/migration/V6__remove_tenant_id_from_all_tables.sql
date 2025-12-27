-- V6: Remove tenant_id columns to align with base-service architecture
-- This migration removes tenant_id from all tables to match the single-project architecture
-- Similar to base-service V22__remove_tenant_architecture.sql

-- Step 1: Drop indexes that include tenant_id
DROP INDEX IF EXISTS card.idx_resident_card_registration_tenant_id;
DROP INDEX IF EXISTS card.idx_resident_card_registration_tenant_status;
DROP INDEX IF EXISTS card.idx_resident_card_registration_tenant_user;
DROP INDEX IF EXISTS card.idx_elevator_card_registration_tenant_id;
DROP INDEX IF EXISTS card.idx_elevator_card_registration_tenant_status;
DROP INDEX IF EXISTS card.idx_elevator_card_registration_tenant_user;
DROP INDEX IF EXISTS card.idx_register_vehicle_tenant_id;
DROP INDEX IF EXISTS card.idx_register_vehicle_tenant_status;
DROP INDEX IF EXISTS card.idx_register_vehicle_tenant_user;

-- Step 2: Drop tenant_id columns from all tables
ALTER TABLE IF EXISTS card.resident_card_registration 
    DROP COLUMN IF EXISTS tenant_id;

ALTER TABLE IF EXISTS card.elevator_card_registration 
    DROP COLUMN IF EXISTS tenant_id;

ALTER TABLE IF EXISTS card.register_vehicle 
    DROP COLUMN IF EXISTS tenant_id;



































