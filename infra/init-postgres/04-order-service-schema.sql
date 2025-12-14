-- ============================================================
-- ORDER SERVICE DATABASE SCHEMA
-- ============================================================
-- Service: Order Management
-- Database: order_service_db
-- ============================================================

\c order_service_db;

-- ============================================================
-- ENUM TYPES
-- ============================================================

CREATE TYPE order_status AS ENUM (
    'pending_payment',       -- Chờ thanh toán (online payment)
    'pending_confirmation',  -- Chờ xác nhận (COD)
    'confirmed',            -- Đã xác nhận
    'waiting_pickup',       -- Chờ lấy hàng
    'shipping',             -- Đang giao
    'delivered',            -- Đã giao thành công
    'completed',            -- Hoàn thành (sau khi hết thời gian khiếu nại)
    'cancelled',            -- Đã hủy
    'returned',             -- Đã trả hàng
    'refunded'              -- Đã hoàn tiền
);

CREATE TYPE payment_method AS ENUM (
    'cod',                  -- Cash on Delivery
    'bank_transfer',        -- Chuyển khoản
    'vnpay',               -- VNPay
    'momo',                -- MoMo
    'zalopay'              -- ZaloPay
);

-- ============================================================
-- TABLE: orders
-- ============================================================
-- Core orders table

CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    order_code VARCHAR(50) UNIQUE NOT NULL, -- VD: ORD-20251214-0001
    
    -- Customer
    user_id INT NOT NULL,
    
    -- Status
    status order_status DEFAULT 'pending_confirmation',
    
    -- Amounts
    subtotal_amount DECIMAL(15, 2) NOT NULL,      -- Tổng giá sản phẩm
    shipping_fee DECIMAL(15, 2) DEFAULT 0,         -- Phí vận chuyển
    discount_amount DECIMAL(15, 2) DEFAULT 0,      -- Giảm giá từ voucher
    total_amount DECIMAL(15, 2) NOT NULL,          -- Tổng trước giảm giá
    final_amount DECIMAL(15, 2) NOT NULL,          -- Tổng sau giảm giá (khách phải trả)
    
    -- Voucher
    voucher_id INT,
    voucher_code VARCHAR(50),
    
    -- Shipping Address Snapshot (JSONB for flexibility)
    shipping_address_snapshot JSONB NOT NULL,
    /*
    Example:
    {
        "recipient_name": "Nguyen Van A",
        "phone": "0901234567",
        "province": "Hà Nội",
        "district": "Cầu Giấy",
        "ward": "Dịch Vọng",
        "detail": "Số 1, Ngõ 2, Đường ABC"
    }
    */
    
    -- Payment
    payment_method payment_method NOT NULL,
    
    -- Shipping
    carrier VARCHAR(50),                -- GHN, GHTK, ViettelPost
    tracking_code VARCHAR(100),
    estimated_delivery_at TIMESTAMP,
    
    -- Notes
    customer_note TEXT,
    admin_note TEXT,
    cancellation_reason TEXT,
    
    -- Timestamps
    confirmed_at TIMESTAMP,
    shipped_at TIMESTAMP,
    delivered_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for orders table
CREATE INDEX idx_orders_code ON orders(order_code);
CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_orders_payment_method ON orders(payment_method);

-- ============================================================
-- TABLE: order_items
-- ============================================================
-- Line items for each order

CREATE TABLE order_items (
    id SERIAL PRIMARY KEY,
    order_id INT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    
    -- Product Reference (MongoDB ObjectId)
    product_id VARCHAR(50) NOT NULL,
    
    -- Product Snapshot (frozen at time of purchase)
    product_name_snapshot VARCHAR(255) NOT NULL,
    product_image_snapshot VARCHAR(500),
    product_attributes_snapshot JSONB, -- {"pages": 464, "cover_type": "Hardcover"}
    
    -- Pricing
    unit_price DECIMAL(15, 2) NOT NULL,    -- Giá đơn vị tại thời điểm mua
    quantity INT NOT NULL CHECK (quantity > 0),
    total_price DECIMAL(15, 2) NOT NULL,    -- unit_price * quantity
    
    -- Review tracking
    is_reviewed BOOLEAN DEFAULT FALSE,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for order_items table
CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_product ON order_items(product_id);

-- ============================================================
-- TABLE: order_status_history
-- ============================================================
-- Tracks all status changes for an order

CREATE TABLE order_status_history (
    id SERIAL PRIMARY KEY,
    order_id INT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    
    -- Status Change
    from_status order_status,
    to_status order_status NOT NULL,
    
    -- Actor
    changed_by INT,             -- User ID (null for system)
    changed_by_role VARCHAR(20), -- customer, admin, system
    
    -- Details
    note TEXT,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for order_status_history
CREATE INDEX idx_status_history_order ON order_status_history(order_id);
CREATE INDEX idx_status_history_created ON order_status_history(created_at);

-- ============================================================
-- TABLE: vouchers
-- ============================================================
-- Discount vouchers/coupons

CREATE TABLE vouchers (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    
    -- Discount Type
    discount_type VARCHAR(20) NOT NULL, -- 'percentage', 'fixed_amount'
    discount_value DECIMAL(15, 2) NOT NULL,
    max_discount_amount DECIMAL(15, 2), -- Cap for percentage discounts
    
    -- Conditions
    min_order_value DECIMAL(15, 2) DEFAULT 0,
    applicable_categories JSONB, -- [1, 2, 3] or null for all
    applicable_products JSONB,   -- ["prod_id_1", "prod_id_2"] or null for all
    
    -- Usage Limits
    max_uses INT,                -- Total uses allowed
    max_uses_per_user INT DEFAULT 1,
    usage_count INT DEFAULT 0,   -- Current usage count
    
    -- Validity
    valid_from TIMESTAMP NOT NULL,
    valid_to TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    
    -- Metadata
    description TEXT,
    created_by INT,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for vouchers table
CREATE INDEX idx_vouchers_code ON vouchers(code);
CREATE INDEX idx_vouchers_valid ON vouchers(valid_from, valid_to);
CREATE INDEX idx_vouchers_active ON vouchers(is_active);

-- ============================================================
-- TABLE: voucher_usages
-- ============================================================
-- Tracks which users have used which vouchers

CREATE TABLE voucher_usages (
    id SERIAL PRIMARY KEY,
    voucher_id INT NOT NULL REFERENCES vouchers(id),
    user_id INT NOT NULL,
    order_id INT NOT NULL REFERENCES orders(id),
    discount_applied DECIMAL(15, 2) NOT NULL,
    used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for voucher_usages
CREATE INDEX idx_voucher_usages_voucher ON voucher_usages(voucher_id);
CREATE INDEX idx_voucher_usages_user ON voucher_usages(user_id);
CREATE UNIQUE INDEX idx_voucher_usages_unique ON voucher_usages(voucher_id, order_id);

-- ============================================================
-- TABLE: shipping_tracking
-- ============================================================
-- Tracks shipping status updates from carriers

CREATE TABLE shipping_tracking (
    id SERIAL PRIMARY KEY,
    order_id INT NOT NULL REFERENCES orders(id),
    
    -- Carrier Info
    carrier VARCHAR(50) NOT NULL,
    tracking_code VARCHAR(100) NOT NULL,
    
    -- Status
    status VARCHAR(50) NOT NULL,
    status_description TEXT,
    location VARCHAR(255),
    
    -- Timestamps
    carrier_timestamp TIMESTAMP,  -- Time from carrier
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for shipping_tracking
CREATE INDEX idx_shipping_order ON shipping_tracking(order_id);
CREATE INDEX idx_shipping_tracking_code ON shipping_tracking(tracking_code);
CREATE INDEX idx_shipping_created ON shipping_tracking(created_at);

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

CREATE TRIGGER update_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_vouchers_updated_at
    BEFORE UPDATE ON vouchers
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- TRIGGER: Record order status changes
-- ============================================================

CREATE OR REPLACE FUNCTION record_order_status_change()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        INSERT INTO order_status_history (order_id, from_status, to_status, changed_by_role)
        VALUES (NEW.id, OLD.status, NEW.status, 'system');
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER order_status_change_trigger
    AFTER UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION record_order_status_change();

-- ============================================================
-- FUNCTION: Generate Order Code
-- ============================================================

CREATE OR REPLACE FUNCTION generate_order_code()
RETURNS VARCHAR(50) AS $$
DECLARE
    v_date VARCHAR(8);
    v_seq INT;
    v_code VARCHAR(50);
BEGIN
    v_date := TO_CHAR(CURRENT_DATE, 'YYYYMMDD');
    
    SELECT COALESCE(MAX(
        CAST(SUBSTRING(order_code FROM 14) AS INT)
    ), 0) + 1
    INTO v_seq
    FROM orders
    WHERE order_code LIKE 'ORD-' || v_date || '-%';
    
    v_code := 'ORD-' || v_date || '-' || LPAD(v_seq::TEXT, 4, '0');
    
    RETURN v_code;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- COMMENTS
-- ============================================================

COMMENT ON TABLE orders IS 'Customer orders with shipping and payment info';
COMMENT ON TABLE order_items IS 'Line items for each order with product snapshots';
COMMENT ON TABLE order_status_history IS 'Audit trail for order status changes';
COMMENT ON TABLE vouchers IS 'Discount vouchers and coupons';
COMMENT ON TABLE voucher_usages IS 'Tracks voucher usage per user';
COMMENT ON TABLE shipping_tracking IS 'Shipping status updates from carriers';
