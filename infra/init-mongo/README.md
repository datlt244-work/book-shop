# MongoDB Database Initialization Scripts

## Overview

Thư mục này chứa các script JavaScript để khởi tạo MongoDB collections và sample data cho hệ thống E-commerce Book Shop.

## File Structure

```
init-mongo/
├── 01-init-collections.js    # Tạo collections, indexes, sample data
└── README.md
```

## Collections

### Product Database (`product_db`)

| Collection | Description |
|------------|-------------|
| `books` | Catalog sách với flexible schema |
| `categories` | Danh mục sách (hierarchical) |
| `authors` | Thông tin tác giả |
| `publishers` | Thông tin nhà xuất bản |
| `reviews` | Đánh giá sản phẩm từ khách hàng |
| `return_requests` | Yêu cầu trả hàng với evidence |
| `banners` | Banner quảng cáo trang chủ |
| `system_logs` | Audit logs (với TTL 90 ngày) |

## Schema Design Patterns

### 1. Embedded Documents
Thay vì tách bảng, nhúng trực tiếp:
```javascript
// images nhúng trong books
images: [
  { url: "...", is_cover: true }
]
```

### 2. Denormalization
Lưu cả ID và Name để tránh join:
```javascript
category: {
  id: 5,
  name: "Programming",
  slug: "programming"
}
```

### 3. Flexible Attributes
Schema linh hoạt cho các loại sản phẩm khác nhau:
```javascript
attributes: {
  pages: 464,
  cover_type: "Hardcover",
  language: "English"
}
```

### 4. TTL Index
Tự động xóa logs cũ:
```javascript
db.system_logs.createIndex(
  { "timestamp": 1 }, 
  { expireAfterSeconds: 7776000 }  // 90 days
);
```

## Indexes

### Books Collection
- `isbn` - Unique
- `slug` - Unique  
- `title, description` - Text search
- `category.id`, `authors.id`, `publisher.id` - Filtering
- `price.sale`, `rating_average`, `sold_count` - Sorting
- `created_at` - Pagination

### Reviews Collection
- `book_id` - Filter reviews by book
- `user_id` - Filter reviews by user
- `book_id + user_id` - Unique (1 review per user per book)
- `status` - Moderation filtering

## Usage

### Development (Docker Compose)

```bash
# Start với init scripts
docker-compose up -d mongodb

# Kiểm tra logs
docker logs ecom-mongo

# Connect vào database
docker exec -it ecom-mongo mongosh -u admin -p password --authenticationDatabase admin
```

### Manual Execution

```bash
# Chạy script thủ công
mongosh -u admin -p password --authenticationDatabase admin < 01-init-collections.js
```

## Sample Data

Script tự động tạo sample data:
- 5 Categories (Văn học, Kinh tế, CNTT, Thiếu nhi, Tâm lý)
- 3 Authors (Nguyễn Nhật Ánh, Dale Carnegie, Robert C. Martin)
- 3 Publishers (NXB Trẻ, NXB Kim Đồng, Alpha Books)
- 3 Books (Clean Code, Đắc Nhân Tâm, Cho Tôi Xin Một Vé Đi Tuổi Thơ)

## Cross-Database References

MongoDB lưu product data, nhưng:
- `inventory` (stock) nằm ở PostgreSQL
- `order_items` reference `book._id` (ObjectId as string)

```
MongoDB                     PostgreSQL
┌─────────────┐            ┌─────────────────┐
│ books       │            │ inventory       │
│ _id: ObjectId ◀─────────▶ product_id: str │
└─────────────┘            └─────────────────┘
```

## Notes

- Scripts chỉ chạy 1 lần khi container init lần đầu
- Để re-run, cần xóa volume: `docker-compose down -v`
- MongoDB init scripts chạy với user root
