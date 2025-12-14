-- ============================================================
-- INVENTORY SERVICE DATABASE SCHEMA
-- ============================================================
-- Service: Inventory Management
-- Database: inventory_service_db
-- ============================================================
-- NOTE: Product info is stored in MongoDB (Product Service)
--       This service only manages stock quantities for ACID transactions
-- ============================================================

\c inventory_service_db;

-- ============================================================
-- ENUM TYPES
-- ============================================================

CREATE TYPE stock_movement_type AS ENUM (
    'purchase',           -- Nhập hàng từ NCC
    'sale',              -- Bán hàng
    'return',            -- Khách trả hàng
    'adjustment',        -- Điều chỉnh thủ công
    'reservation',       -- Đặt trước (giữ hàng trong giỏ)
    'reservation_release', -- Hủy đặt trước
    'damaged',           -- Hàng hỏng
    'transfer'           -- Chuyển kho
);

CREATE TYPE stock_alert_status AS ENUM ('pending', 'acknowledged', 'resolved');

-- ============================================================
-- TABLE: inventory
-- ============================================================
-- Core inventory table - tracks stock for each product
-- product_id references MongoDB ObjectId (stored as string)

CREATE TABLE inventory (
    id SERIAL PRIMARY KEY,
    product_id VARCHAR(50) UNIQUE NOT NULL, -- MongoDB ObjectId as string
    
    -- Stock Quantities
    stock_quantity INT DEFAULT 0 CHECK (stock_quantity >= 0),
    reserved_quantity INT DEFAULT 0 CHECK (reserved_quantity >= 0),
    
    -- Alerts
    low_stock_threshold INT DEFAULT 5,
    is_low_stock BOOLEAN GENERATED ALWAYS AS (stock_quantity - reserved_quantity <= low_stock_threshold) STORED,
    
    -- Tracking
    last_restock_at TIMESTAMP,
    last_sale_at TIMESTAMP,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for inventory table
CREATE INDEX idx_inventory_product_id ON inventory(product_id);
CREATE INDEX idx_inventory_low_stock ON inventory(is_low_stock) WHERE is_low_stock = TRUE;
CREATE INDEX idx_inventory_stock_qty ON inventory(stock_quantity);

-- ============================================================
-- TABLE: stock_movements
-- ============================================================
-- Tracks all stock changes for audit and reporting

CREATE TABLE stock_movements (
    id SERIAL PRIMARY KEY,
    inventory_id INT NOT NULL REFERENCES inventory(id),
    product_id VARCHAR(50) NOT NULL,
    
    -- Movement Details
    movement_type stock_movement_type NOT NULL,
    quantity INT NOT NULL, -- Positive for increase, negative for decrease
    
    -- Reference
    reference_type VARCHAR(50), -- 'order', 'purchase_order', 'manual', etc.
    reference_id VARCHAR(50),   -- Order ID, PO ID, etc.
    
    -- Before/After for audit
    stock_before INT NOT NULL,
    stock_after INT NOT NULL,
    
    -- Actor
    created_by INT, -- User ID who made the change
    note TEXT,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for stock_movements table
CREATE INDEX idx_movements_inventory ON stock_movements(inventory_id);
CREATE INDEX idx_movements_product ON stock_movements(product_id);
CREATE INDEX idx_movements_type ON stock_movements(movement_type);
CREATE INDEX idx_movements_reference ON stock_movements(reference_type, reference_id);
CREATE INDEX idx_movements_created_at ON stock_movements(created_at);

-- ============================================================
-- TABLE: stock_reservations
-- ============================================================
-- Tracks reserved stock (items in cart, pending orders)
-- Prevents overselling with reservation pattern

CREATE TABLE stock_reservations (
    id SERIAL PRIMARY KEY,
    inventory_id INT NOT NULL REFERENCES inventory(id),
    product_id VARCHAR(50) NOT NULL,
    
    -- Reservation Details
    quantity INT NOT NULL CHECK (quantity > 0),
    
    -- Reference
    reference_type VARCHAR(50) NOT NULL, -- 'cart', 'order'
    reference_id VARCHAR(50) NOT NULL,   -- Cart ID or Order ID
    user_id INT,
    
    -- Status
    status VARCHAR(20) DEFAULT 'active', -- active, released, converted
    
    -- Expiry (for cart reservations)
    expires_at TIMESTAMP,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    released_at TIMESTAMP
);

-- Indexes for stock_reservations table
CREATE INDEX idx_reservations_inventory ON stock_reservations(inventory_id);
CREATE INDEX idx_reservations_product ON stock_reservations(product_id);
CREATE INDEX idx_reservations_reference ON stock_reservations(reference_type, reference_id);
CREATE INDEX idx_reservations_status ON stock_reservations(status);
CREATE INDEX idx_reservations_expires ON stock_reservations(expires_at) WHERE status = 'active';

-- ============================================================
-- TABLE: stock_alerts
-- ============================================================
-- Low stock alerts for admin notification

CREATE TABLE stock_alerts (
    id SERIAL PRIMARY KEY,
    inventory_id INT NOT NULL REFERENCES inventory(id),
    product_id VARCHAR(50) NOT NULL,
    
    -- Alert Details
    alert_type VARCHAR(50) DEFAULT 'low_stock',
    current_stock INT NOT NULL,
    threshold INT NOT NULL,
    
    -- Status
    status stock_alert_status DEFAULT 'pending',
    acknowledged_by INT,
    acknowledged_at TIMESTAMP,
    resolved_at TIMESTAMP,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for stock_alerts table
CREATE INDEX idx_alerts_inventory ON stock_alerts(inventory_id);
CREATE INDEX idx_alerts_status ON stock_alerts(status);
CREATE INDEX idx_alerts_created_at ON stock_alerts(created_at);

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

CREATE TRIGGER update_inventory_updated_at
    BEFORE UPDATE ON inventory
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- FUNCTION: Reserve Stock
-- ============================================================
-- Atomic function to reserve stock (used when adding to cart)

CREATE OR REPLACE FUNCTION reserve_stock(
    p_product_id VARCHAR(50),
    p_quantity INT,
    p_reference_type VARCHAR(50),
    p_reference_id VARCHAR(50),
    p_user_id INT,
    p_expires_minutes INT DEFAULT 30
)
RETURNS BOOLEAN AS $$
DECLARE
    v_inventory_id INT;
    v_available INT;
BEGIN
    -- Get inventory record with lock
    SELECT id, stock_quantity - reserved_quantity
    INTO v_inventory_id, v_available
    FROM inventory
    WHERE product_id = p_product_id
    FOR UPDATE;
    
    -- Check if enough stock available
    IF v_available IS NULL OR v_available < p_quantity THEN
        RETURN FALSE;
    END IF;
    
    -- Update reserved quantity
    UPDATE inventory
    SET reserved_quantity = reserved_quantity + p_quantity
    WHERE id = v_inventory_id;
    
    -- Create reservation record
    INSERT INTO stock_reservations (
        inventory_id, product_id, quantity,
        reference_type, reference_id, user_id,
        expires_at
    ) VALUES (
        v_inventory_id, p_product_id, p_quantity,
        p_reference_type, p_reference_id, p_user_id,
        CURRENT_TIMESTAMP + (p_expires_minutes || ' minutes')::INTERVAL
    );
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- FUNCTION: Release Reservation
-- ============================================================
-- Release reserved stock (when cart expires or order cancelled)

CREATE OR REPLACE FUNCTION release_reservation(
    p_reference_type VARCHAR(50),
    p_reference_id VARCHAR(50)
)
RETURNS INT AS $$
DECLARE
    v_released_count INT := 0;
    v_reservation RECORD;
BEGIN
    FOR v_reservation IN
        SELECT id, inventory_id, quantity
        FROM stock_reservations
        WHERE reference_type = p_reference_type
          AND reference_id = p_reference_id
          AND status = 'active'
        FOR UPDATE
    LOOP
        -- Update inventory
        UPDATE inventory
        SET reserved_quantity = reserved_quantity - v_reservation.quantity
        WHERE id = v_reservation.inventory_id;
        
        -- Mark reservation as released
        UPDATE stock_reservations
        SET status = 'released', released_at = CURRENT_TIMESTAMP
        WHERE id = v_reservation.id;
        
        v_released_count := v_released_count + 1;
    END LOOP;
    
    RETURN v_released_count;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- FUNCTION: Deduct Stock (Convert reservation to sale)
-- ============================================================

CREATE OR REPLACE FUNCTION deduct_stock(
    p_product_id VARCHAR(50),
    p_quantity INT,
    p_order_id VARCHAR(50),
    p_user_id INT DEFAULT NULL
)
RETURNS BOOLEAN AS $$
DECLARE
    v_inventory_id INT;
    v_stock_before INT;
    v_stock_after INT;
BEGIN
    -- Get inventory record with lock
    SELECT id, stock_quantity
    INTO v_inventory_id, v_stock_before
    FROM inventory
    WHERE product_id = p_product_id
    FOR UPDATE;
    
    IF v_inventory_id IS NULL THEN
        RETURN FALSE;
    END IF;
    
    v_stock_after := v_stock_before - p_quantity;
    
    -- Update stock
    UPDATE inventory
    SET stock_quantity = v_stock_after,
        reserved_quantity = GREATEST(0, reserved_quantity - p_quantity),
        last_sale_at = CURRENT_TIMESTAMP
    WHERE id = v_inventory_id;
    
    -- Record movement
    INSERT INTO stock_movements (
        inventory_id, product_id, movement_type, quantity,
        reference_type, reference_id,
        stock_before, stock_after, created_by
    ) VALUES (
        v_inventory_id, p_product_id, 'sale', -p_quantity,
        'order', p_order_id,
        v_stock_before, v_stock_after, p_user_id
    );
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- COMMENTS
-- ============================================================

COMMENT ON TABLE inventory IS 'Core inventory tracking - stock quantities per product';
COMMENT ON TABLE stock_movements IS 'Audit log for all stock changes';
COMMENT ON TABLE stock_reservations IS 'Reserved stock for cart items and pending orders';
COMMENT ON TABLE stock_alerts IS 'Low stock alerts for admin notification';
COMMENT ON FUNCTION reserve_stock IS 'Atomically reserve stock when adding to cart';
COMMENT ON FUNCTION release_reservation IS 'Release reserved stock when cart expires or order cancelled';
COMMENT ON FUNCTION deduct_stock IS 'Deduct stock when order is confirmed';
