# PostgreSQL Database Initialization Scripts

## Overview

Thư mục này chứa các script SQL để khởi tạo database schema cho hệ thống E-commerce Book Shop.

## File Structure

```
init-postgres/
├── 01-create-databases.sql      # Tạo các databases
├── 02-user-service-schema.sql   # Schema cho User Service
├── 03-inventory-service-schema.sql  # Schema cho Inventory Service
├── 04-order-service-schema.sql  # Schema cho Order Service
├── 05-payment-service-schema.sql    # Schema cho Payment Service
└── README.md
```

## Database Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     PostgreSQL Server                            │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │ user_service_db │  │inventory_svc_db │  │ order_svc_db    │ │
│  ├─────────────────┤  ├─────────────────┤  ├─────────────────┤ │
│  │ • users         │  │ • inventory     │  │ • orders        │ │
│  │ • addresses     │  │ • stock_move... │  │ • order_items   │ │
│  │ • user_sessions │  │ • stock_reser.. │  │ • vouchers      │ │
│  │ • password_...  │  │ • stock_alerts  │  │ • voucher_...   │ │
│  └─────────────────┘  └─────────────────┘  │ • shipping_...  │ │
│                                             └─────────────────┘ │
│  ┌─────────────────┐                                            │
│  │ payment_svc_db  │                                            │
│  ├─────────────────┤                                            │
│  │ • payments      │                                            │
│  │ • payment_logs  │                                            │
│  │ • refunds       │                                            │
│  │ • bank_accounts │                                            │
│  └─────────────────┘                                            │
└─────────────────────────────────────────────────────────────────┘
```

## How It Works

Docker PostgreSQL sẽ tự động chạy các file `.sql` trong thư mục `/docker-entrypoint-initdb.d/` theo thứ tự alphabet khi container khởi tạo lần đầu.

## Tables Summary

### User Service Database (`user_service_db`)

| Table | Description |
|-------|-------------|
| `users` | User accounts với authentication & authorization |
| `addresses` | Địa chỉ giao hàng của user |
| `user_sessions` | Theo dõi phiên đăng nhập |
| `password_reset_tokens` | Token reset mật khẩu |

### Inventory Service Database (`inventory_service_db`)

| Table | Description |
|-------|-------------|
| `inventory` | Số lượng tồn kho cho mỗi sản phẩm |
| `stock_movements` | Lịch sử thay đổi kho (audit) |
| `stock_reservations` | Hàng đang được giữ (trong giỏ/đơn chờ) |
| `stock_alerts` | Cảnh báo hết hàng |

### Order Service Database (`order_service_db`)

| Table | Description |
|-------|-------------|
| `orders` | Đơn hàng chính |
| `order_items` | Chi tiết sản phẩm trong đơn |
| `order_status_history` | Lịch sử thay đổi trạng thái |
| `vouchers` | Mã giảm giá |
| `voucher_usages` | Theo dõi sử dụng voucher |
| `shipping_tracking` | Theo dõi vận chuyển |

### Payment Service Database (`payment_service_db`)

| Table | Description |
|-------|-------------|
| `payments` | Giao dịch thanh toán |
| `payment_logs` | Log sự kiện thanh toán |
| `refunds` | Yêu cầu hoàn tiền |
| `bank_accounts` | Tài khoản ngân hàng shop |
| `payment_provider_configs` | Cấu hình payment gateway |

## Key Features

### 1. ENUM Types
Sử dụng PostgreSQL ENUM cho type safety:
- `user_role`: customer, staff, admin, super_admin
- `order_status`: pending_payment, confirmed, shipping, delivered, etc.
- `payment_status`: pending, completed, failed, refunded

### 2. Auto-update Triggers
Tất cả các bảng có trigger tự động cập nhật `updated_at`:
```sql
CREATE TRIGGER update_xxx_updated_at
    BEFORE UPDATE ON xxx
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

### 3. Stored Functions
- `reserve_stock()`: Đặt trước hàng khi thêm vào giỏ
- `release_reservation()`: Giải phóng hàng khi hủy
- `deduct_stock()`: Trừ kho khi đơn hàng confirmed
- `generate_order_code()`: Tạo mã đơn hàng tự động

### 4. Snapshot Pattern
Lưu thông tin tại thời điểm mua để đảm bảo tính chính xác:
- `shipping_address_snapshot` (JSONB)
- `product_name_snapshot`
- `product_image_snapshot`

### 5. Reserved Quantity Pattern
Tránh overselling bằng cách track:
```
available_to_sell = stock_quantity - reserved_quantity
```

## Usage

### Development (Docker Compose)

```bash
# Start với init scripts
docker-compose up -d postgres

# Kiểm tra logs
docker logs ecom-postgres

# Connect vào database
docker exec -it ecom-postgres psql -U postgres -d user_service_db
```

### Manual Execution

```bash
# Nếu cần chạy lại scripts
psql -U postgres -f 01-create-databases.sql
psql -U postgres -f 02-user-service-schema.sql
# ... etc
```

## Cross-Database References

Do mỗi service có database riêng, các references giữa services sử dụng:

1. **Logical References**: Lưu ID mà không tạo FK constraint
   ```sql
   user_id INT NOT NULL  -- References users.id in user_service_db
   ```

2. **String References cho MongoDB**: 
   ```sql
   product_id VARCHAR(50)  -- MongoDB ObjectId as string
   ```

## Notes

- Scripts chỉ chạy 1 lần khi container init lần đầu
- Để re-run, cần xóa volume: `docker-compose down -v`
- Không sử dụng FK constraints giữa các databases (microservices pattern)
