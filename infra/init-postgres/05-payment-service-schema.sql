-- ============================================================
-- PAYMENT SERVICE DATABASE SCHEMA
-- ============================================================
-- Service: Payment Processing
-- Database: payment_service_db
-- ============================================================

\c payment_service_db;

-- ============================================================
-- ENUM TYPES
-- ============================================================

CREATE TYPE payment_status AS ENUM (
    'pending',          -- Chờ thanh toán
    'processing',       -- Đang xử lý
    'completed',        -- Thành công
    'failed',           -- Thất bại
    'cancelled',        -- Đã hủy
    'refund_pending',   -- Chờ hoàn tiền
    'refunded',         -- Đã hoàn tiền
    'partially_refunded' -- Hoàn tiền một phần
);

CREATE TYPE payment_method_type AS ENUM (
    'cod',
    'bank_transfer',
    'vnpay',
    'momo',
    'zalopay'
);

CREATE TYPE refund_status AS ENUM (
    'pending',
    'processing',
    'approved',
    'rejected',
    'completed'
);

-- ============================================================
-- TABLE: payments
-- ============================================================
-- Core payment transactions

CREATE TABLE payments (
    id SERIAL PRIMARY KEY,
    payment_code VARCHAR(50) UNIQUE NOT NULL, -- PAY-20251214-0001
    
    -- Order Reference
    order_id INT NOT NULL,
    order_code VARCHAR(50) NOT NULL,
    user_id INT NOT NULL,
    
    -- Payment Details
    method payment_method_type NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'VND',
    
    -- Status
    status payment_status DEFAULT 'pending',
    
    -- Provider Response
    provider_transaction_id VARCHAR(255),  -- Transaction ID from payment provider
    provider_response JSONB,               -- Full response from provider
    
    -- Bank Transfer specific
    bank_code VARCHAR(20),
    bank_account VARCHAR(50),
    
    -- Timestamps
    paid_at TIMESTAMP,
    expired_at TIMESTAMP,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for payments table
CREATE INDEX idx_payments_code ON payments(payment_code);
CREATE INDEX idx_payments_order ON payments(order_id);
CREATE INDEX idx_payments_user ON payments(user_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_method ON payments(method);
CREATE INDEX idx_payments_created ON payments(created_at);
CREATE INDEX idx_payments_provider_txn ON payments(provider_transaction_id);

-- ============================================================
-- TABLE: payment_logs
-- ============================================================
-- Logs all payment-related events for audit

CREATE TABLE payment_logs (
    id SERIAL PRIMARY KEY,
    payment_id INT NOT NULL REFERENCES payments(id),
    
    -- Event Details
    event_type VARCHAR(50) NOT NULL, -- 'initiated', 'callback', 'status_change', 'error'
    event_data JSONB,
    
    -- Status tracking
    from_status payment_status,
    to_status payment_status,
    
    -- Source
    source VARCHAR(50), -- 'user', 'system', 'provider_callback'
    ip_address VARCHAR(45),
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for payment_logs
CREATE INDEX idx_payment_logs_payment ON payment_logs(payment_id);
CREATE INDEX idx_payment_logs_event ON payment_logs(event_type);
CREATE INDEX idx_payment_logs_created ON payment_logs(created_at);

-- ============================================================
-- TABLE: refunds
-- ============================================================
-- Refund requests and processing

CREATE TABLE refunds (
    id SERIAL PRIMARY KEY,
    refund_code VARCHAR(50) UNIQUE NOT NULL, -- REF-20251214-0001
    
    -- References
    payment_id INT NOT NULL REFERENCES payments(id),
    order_id INT NOT NULL,
    user_id INT NOT NULL,
    
    -- Refund Details
    amount DECIMAL(15, 2) NOT NULL,
    reason TEXT NOT NULL,
    
    -- Status
    status refund_status DEFAULT 'pending',
    
    -- Processing
    processed_by INT,            -- Admin user ID
    processed_at TIMESTAMP,
    rejection_reason TEXT,
    
    -- Provider Response
    provider_refund_id VARCHAR(255),
    provider_response JSONB,
    
    -- Bank Details (for bank transfer refunds)
    refund_bank_code VARCHAR(20),
    refund_bank_account VARCHAR(50),
    refund_account_name VARCHAR(100),
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for refunds
CREATE INDEX idx_refunds_code ON refunds(refund_code);
CREATE INDEX idx_refunds_payment ON refunds(payment_id);
CREATE INDEX idx_refunds_order ON refunds(order_id);
CREATE INDEX idx_refunds_user ON refunds(user_id);
CREATE INDEX idx_refunds_status ON refunds(status);
CREATE INDEX idx_refunds_created ON refunds(created_at);

-- ============================================================
-- TABLE: bank_accounts
-- ============================================================
-- Shop's bank accounts for receiving payments

CREATE TABLE bank_accounts (
    id SERIAL PRIMARY KEY,
    
    -- Bank Info
    bank_code VARCHAR(20) NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    account_name VARCHAR(100) NOT NULL,
    branch VARCHAR(100),
    
    -- Status
    is_active BOOLEAN DEFAULT TRUE,
    is_primary BOOLEAN DEFAULT FALSE,
    
    -- Display
    display_order INT DEFAULT 0,
    qr_code_url VARCHAR(500),
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- TABLE: payment_provider_configs
-- ============================================================
-- Configuration for payment providers (VNPay, MoMo, etc.)

CREATE TABLE payment_provider_configs (
    id SERIAL PRIMARY KEY,
    provider VARCHAR(50) UNIQUE NOT NULL, -- vnpay, momo, zalopay
    
    -- Status
    is_enabled BOOLEAN DEFAULT FALSE,
    
    -- Config (encrypted in production)
    config_data JSONB NOT NULL,
    /*
    Example for VNPay:
    {
        "tmn_code": "xxx",
        "hash_secret": "xxx",
        "api_url": "https://...",
        "return_url": "https://...",
        "ipn_url": "https://..."
    }
    */
    
    -- Environment
    environment VARCHAR(20) DEFAULT 'sandbox', -- sandbox, production
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- TRIGGER: Auto-update updated_at
-- ============================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_refunds_updated_at
    BEFORE UPDATE ON refunds
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_bank_accounts_updated_at
    BEFORE UPDATE ON bank_accounts
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_provider_configs_updated_at
    BEFORE UPDATE ON payment_provider_configs
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- FUNCTION: Generate Payment Code
-- ============================================================

CREATE OR REPLACE FUNCTION generate_payment_code()
RETURNS VARCHAR(50) AS $$
DECLARE
    v_date VARCHAR(8);
    v_seq INT;
BEGIN
    v_date := TO_CHAR(CURRENT_DATE, 'YYYYMMDD');
    
    SELECT COALESCE(MAX(
        CAST(SUBSTRING(payment_code FROM 14) AS INT)
    ), 0) + 1
    INTO v_seq
    FROM payments
    WHERE payment_code LIKE 'PAY-' || v_date || '-%';
    
    RETURN 'PAY-' || v_date || '-' || LPAD(v_seq::TEXT, 4, '0');
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- FUNCTION: Generate Refund Code
-- ============================================================

CREATE OR REPLACE FUNCTION generate_refund_code()
RETURNS VARCHAR(50) AS $$
DECLARE
    v_date VARCHAR(8);
    v_seq INT;
BEGIN
    v_date := TO_CHAR(CURRENT_DATE, 'YYYYMMDD');
    
    SELECT COALESCE(MAX(
        CAST(SUBSTRING(refund_code FROM 14) AS INT)
    ), 0) + 1
    INTO v_seq
    FROM refunds
    WHERE refund_code LIKE 'REF-' || v_date || '-%';
    
    RETURN 'REF-' || v_date || '-' || LPAD(v_seq::TEXT, 4, '0');
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- COMMENTS
-- ============================================================

COMMENT ON TABLE payments IS 'Payment transactions for orders';
COMMENT ON TABLE payment_logs IS 'Audit log for all payment events';
COMMENT ON TABLE refunds IS 'Refund requests and processing';
COMMENT ON TABLE bank_accounts IS 'Shop bank accounts for receiving payments';
COMMENT ON TABLE payment_provider_configs IS 'Configuration for payment gateways';
