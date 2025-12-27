-- Migration: V6__add_checkout_date_to_contracts.sql
-- Thêm column checkout_date vào bảng contracts

ALTER TABLE files.contracts
    ADD COLUMN IF NOT EXISTS checkout_date DATE;
