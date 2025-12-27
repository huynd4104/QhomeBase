DO $do$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_type t
    JOIN pg_namespace n ON n.oid = t.typnamespace
    WHERE t.typname = 'refund_status' AND n.nspname = 'finance'
  ) THEN
CREATE TYPE finance.refund_status AS ENUM ('PENDING', 'APPROVED', 'PROCESSING', 'COMPLETED', 'REJECTED', 'CANCELLED');
END IF;
END
$do$;

DO $do$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_type t
    JOIN pg_namespace n ON n.oid = t.typnamespace
    WHERE t.typname = 'ledger_entry_type' AND n.nspname = 'finance'
  ) THEN
CREATE TYPE finance.ledger_entry_type AS ENUM ('CHARGE', 'PAYMENT', 'ADJUSTMENT', 'REFUND', 'LATE_FEE', 'CREDIT');
END IF;
END
$do$;

CREATE TABLE IF NOT EXISTS finance.refunds (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL,
    payment_id        UUID NOT NULL,
    refund_no         TEXT NOT NULL,
    refund_amount     NUMERIC(14,4) NOT NULL,
    reason            TEXT NOT NULL,
    status            finance.refund_status NOT NULL DEFAULT 'PENDING',
    method            finance.payment_method,
    refunded_at       TIMESTAMPTZ,
    requested_by      UUID NOT NULL,
    approved_by       UUID,
    approved_at       TIMESTAMPTZ,
    rejection_reason  TEXT,
    gateway_txn_id    TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT uq_refund_no_per_tenant 
        UNIQUE (tenant_id, refund_no),
    CONSTRAINT fk_refund_payment 
        FOREIGN KEY (payment_id) REFERENCES finance.payments(id) ON DELETE RESTRICT,
    CONSTRAINT ck_refund_amount_positive 
        CHECK (refund_amount > 0)
);

CREATE TABLE IF NOT EXISTS finance.account_balances (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL,
    unit_id           UUID,
    resident_id       UUID,
    balance           NUMERIC(14,4) NOT NULL DEFAULT 0,
    currency          TEXT NOT NULL DEFAULT 'VND',
    last_updated      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT uq_balance_account 
        UNIQUE (tenant_id, unit_id, resident_id),
    CONSTRAINT ck_balance_has_reference
        CHECK (unit_id IS NOT NULL OR resident_id IS NOT NULL)
);

CREATE TABLE IF NOT EXISTS finance.ledger_entries (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL,
    account_id        UUID NOT NULL,
    entry_type        finance.ledger_entry_type NOT NULL,
    amount            NUMERIC(14,4) NOT NULL,
    balance_before    NUMERIC(14,4) NOT NULL,
    balance_after     NUMERIC(14,4) NOT NULL,
    reference_type    TEXT,
    reference_id      UUID,
    description       TEXT,
    metadata          JSONB,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT fk_ledger_account 
        FOREIGN KEY (account_id) REFERENCES finance.account_balances(id) ON DELETE CASCADE,
    CONSTRAINT ck_balance_calculation
        CHECK (balance_after = balance_before + amount)
);

CREATE TABLE IF NOT EXISTS finance.payment_reconciliation (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL,
    reconcile_date    DATE NOT NULL,
    gateway_id        UUID,
    total_payments    INTEGER NOT NULL DEFAULT 0,
    total_amount      NUMERIC(14,4) NOT NULL DEFAULT 0,
    matched_count     INTEGER NOT NULL DEFAULT 0,
    unmatched_count   INTEGER NOT NULL DEFAULT 0,
    status            TEXT NOT NULL DEFAULT 'IN_PROGRESS',
    notes             TEXT,
    reconciled_by     UUID,
    reconciled_at     TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT uq_reconcile_date_gateway 
        UNIQUE (tenant_id, reconcile_date, gateway_id),
    CONSTRAINT fk_reconcile_gateway
        FOREIGN KEY (gateway_id) REFERENCES finance.payment_gateways(id) ON DELETE SET NULL,
    CONSTRAINT ck_reconcile_counts
        CHECK (total_payments = matched_count + unmatched_count)
);

CREATE TABLE IF NOT EXISTS finance.reconciliation_items (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL,
    reconciliation_id UUID NOT NULL,
    payment_id        UUID,
    gateway_txn_id    UUID,
    is_matched        BOOLEAN NOT NULL DEFAULT false,
    discrepancy_amount NUMERIC(14,4),
    notes             TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT fk_recon_item_reconciliation
        FOREIGN KEY (reconciliation_id) REFERENCES finance.payment_reconciliation(id) ON DELETE CASCADE,
    CONSTRAINT fk_recon_item_payment
        FOREIGN KEY (payment_id) REFERENCES finance.payments(id) ON DELETE SET NULL,
    CONSTRAINT fk_recon_item_gateway_txn
        FOREIGN KEY (gateway_txn_id) REFERENCES finance.gateway_transactions(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS finance.cash_accounts (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL,
    account_code      TEXT NOT NULL,
    account_name      TEXT NOT NULL,
    account_type      TEXT NOT NULL,
    bank_name         TEXT,
    bank_branch       TEXT,
    account_number    TEXT,
    currency          TEXT NOT NULL DEFAULT 'VND',
    current_balance   NUMERIC(14,4) NOT NULL DEFAULT 0,
    active            BOOLEAN NOT NULL DEFAULT true,
    description       TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT uq_cash_account_code 
        UNIQUE (tenant_id, account_code),
    CONSTRAINT ck_account_type_valid
        CHECK (account_type IN ('CASH', 'BANK', 'E_WALLET', 'OTHER'))
);

CREATE INDEX IF NOT EXISTS idx_refunds_payment ON finance.refunds (payment_id);
CREATE INDEX IF NOT EXISTS idx_refunds_tenant_status ON finance.refunds (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_refunds_tenant_date ON finance.refunds (tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_account_balances_tenant ON finance.account_balances (tenant_id);
CREATE INDEX IF NOT EXISTS idx_account_balances_unit ON finance.account_balances (unit_id) WHERE unit_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_account_balances_resident ON finance.account_balances (resident_id) WHERE resident_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_ledger_entries_account_date ON finance.ledger_entries (account_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ledger_entries_tenant_date ON finance.ledger_entries (tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ledger_entries_reference ON finance.ledger_entries (reference_type, reference_id);
CREATE INDEX IF NOT EXISTS idx_payment_reconciliation_tenant_date ON finance.payment_reconciliation (tenant_id, reconcile_date DESC);
CREATE INDEX IF NOT EXISTS idx_reconciliation_items_reconciliation ON finance.reconciliation_items (reconciliation_id);
CREATE INDEX IF NOT EXISTS idx_cash_accounts_tenant_active ON finance.cash_accounts (tenant_id, active);

COMMENT ON TABLE finance.refunds IS 'Quản lý hoàn tiền cho khách hàng';
COMMENT ON TABLE finance.account_balances IS 'Số dư tài khoản theo unit/resident';
COMMENT ON TABLE finance.ledger_entries IS 'Sổ cái ghi nhận mọi giao dịch tài chính (audit trail)';
COMMENT ON TABLE finance.payment_reconciliation IS 'Đối soát thanh toán với cổng thanh toán';
COMMENT ON TABLE finance.reconciliation_items IS 'Chi tiết các giao dịch trong đợt đối soát';
COMMENT ON TABLE finance.cash_accounts IS 'Tài khoản tiền mặt/ngân hàng của tenant';

COMMENT ON COLUMN finance.refunds.status IS 'PENDING: Chờ duyệt, APPROVED: Đã duyệt, PROCESSING: Đang xử lý, COMPLETED: Hoàn thành, REJECTED: Từ chối';
COMMENT ON COLUMN finance.account_balances.balance IS 'Số dư hiện tại (âm = nợ, dương = có tiền thừa)';
COMMENT ON COLUMN finance.ledger_entries.amount IS 'Số tiền giao dịch (+ = thu, - = chi)';
COMMENT ON COLUMN finance.ledger_entries.entry_type IS 'CHARGE: Phát sinh phí, PAYMENT: Thanh toán, ADJUSTMENT: Điều chỉnh, REFUND: Hoàn tiền, LATE_FEE: Phí trễ, CREDIT: Ghi có';
COMMENT ON COLUMN finance.cash_accounts.account_type IS 'CASH: Tiền mặt, BANK: Tài khoản ngân hàng, E_WALLET: Ví điện tử';
COMMENT ON COLUMN finance.payment_reconciliation.status IS 'IN_PROGRESS: Đang đối soát, COMPLETED: Hoàn thành, PENDING_REVIEW: Chờ xem xét';




