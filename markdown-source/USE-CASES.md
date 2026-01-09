# 📋 Danh Sách Use Cases - Book Shop E-commerce

> Tài liệu này liệt kê tất cả các Use Cases trong hệ thống, mô tả tác dụng và luồng hoạt động chi tiết.

---

## 📑 Mục Lục

- [1. Authentication Service](#1-authentication-service)
- [2. User Service](#2-user-service)
- [3. API Gateway](#3-api-gateway)
- [4. Service-to-Service Communication](#4-service-to-service-communication)

---

## 1. Authentication Service

> **Port:** 8088 | **Base Path:** `/api/v1/auth`

### UC-01: Đăng Ký Tài Khoản (Register)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `POST /api/v1/auth/register` |
| **Actor** | Guest (chưa đăng nhập) |
| **Mục đích** | Tạo tài khoản mới trong hệ thống |

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "Password123!",
  "fullName": "Nguyễn Văn A",
  "phoneNumber": "0901234567"
}
```

**Luồng hoạt động:**

```
┌─────────┐      ┌──────────────┐      ┌──────────────┐      ┌─────────────┐
│  Client │      │ Auth Service │      │ User Service │      │ Email SMTP  │
└────┬────┘      └──────┬───────┘      └──────┬───────┘      └──────┬──────┘
     │                  │                     │                      │
     │  1. POST /register                     │                      │
     │─────────────────>│                     │                      │
     │                  │                     │                      │
     │                  │ 2. Check email exists                      │
     │                  │─────────────┐       │                      │
     │                  │             │       │                      │
     │                  │<────────────┘       │                      │
     │                  │                     │                      │
     │                  │ 3. Create UserCredential                   │
     │                  │ (status: pending_verification)             │
     │                  │                     │                      │
     │                  │ 4. POST /internal/users                    │
     │                  │────────────────────>│                      │
     │                  │                     │                      │
     │                  │                     │ 5. Create UserProfile│
     │                  │                     │─────────────┐        │
     │                  │                     │<────────────┘        │
     │                  │                     │                      │
     │                  │  6. Profile created │                      │
     │                  │<────────────────────│                      │
     │                  │                     │                      │
     │                  │ 7. Generate verification token             │
     │                  │ 8. Store token in Redis                    │
     │                  │                     │                      │
     │                  │ 9. Send verification email                 │
     │                  │─────────────────────────────────────────-->│
     │                  │                     │                      │
     │ 10. Registration success               │                      │
     │<─────────────────│                     │                      │
     │                  │                     │                      │
```

**Response:**
```json
{
  "code": 200,
  "result": {
    "userId": "uuid",
    "email": "user@example.com",
    "fullName": "Nguyễn Văn A",
    "status": "pending_verification",
    "message": "Registration successful. Please check your email to verify your account."
  }
}
```

---

### UC-02: Đăng Nhập (Login)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `POST /api/v1/auth/login` |
| **Actor** | User đã đăng ký |
| **Mục đích** | Xác thực và lấy JWT token |

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "Password123!"
}
```

**Luồng hoạt động:**

```
┌─────────┐      ┌──────────────┐      ┌───────────┐      ┌──────────────┐
│  Client │      │ Auth Service │      │   Redis   │      │ User Service │
└────┬────┘      └──────┬───────┘      └─────┬─────┘      └──────┬───────┘
     │                  │                    │                   │
     │  1. POST /login  │                    │                   │
     │─────────────────>│                    │                   │
     │                  │                    │                   │
     │                  │ 2. Check rate limit│                   │
     │                  │───────────────────>│                   │
     │                  │    (isRateLimited) │                   │
     │                  │<───────────────────│                   │
     │                  │                    │                   │
     │                  │ 3. Find user by email                  │
     │                  │─────────────┐      │                   │
     │                  │<────────────┘      │                   │
     │                  │                    │                   │
     │                  │ 4. Check status (active, blocked, etc) │
     │                  │                    │                   │
     │                  │ 5. Verify password │                   │
     │                  │                    │                   │
     │                  │ 6. Reset login attempts                │
     │                  │───────────────────>│                   │
     │                  │                    │                   │
     │                  │ 7. Update login tracking               │
     │                  │ (lastLoginAt, loginCount, IP)          │
     │                  │                    │                   │
     │                  │ 8. GET /internal/users/{id}/basic      │
     │                  │────────────────────────────────────────>│
     │                  │    (get fullName, avatarUrl)           │
     │                  │<────────────────────────────────────────│
     │                  │                    │                   │
     │                  │ 9. Generate JWT    │                   │
     │                  │ (accessToken)      │                   │
     │                  │                    │                   │
     │                  │ 10. Create refreshToken                │
     │                  │───────────────────>│                   │
     │                  │    (store in Redis)│                   │
     │                  │                    │                   │
     │ 11. Return tokens│                    │                   │
     │<─────────────────│                    │                   │
     │                  │                    │                   │
```

**Response:**
```json
{
  "code": 200,
  "result": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "rt_abc123...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "userId": "uuid",
    "email": "user@example.com",
    "fullName": "Nguyễn Văn A",
    "avatarUrl": "https://...",
    "role": "customer"
  }
}
```

**Xử lý lỗi:**
- Sai mật khẩu 5 lần → Khóa 15 phút (rate limiting)
- Email chưa verify → Trả về lỗi `EMAIL_NOT_VERIFIED`
- Tài khoản bị khóa → Trả về lỗi `ACCOUNT_BLOCKED`

---

### UC-03: Làm Mới Token (Refresh Token)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `POST /api/v1/auth/refresh` |
| **Actor** | Authenticated User |
| **Mục đích** | Lấy access token mới khi token cũ hết hạn |

**Luồng hoạt động:**

```
┌─────────┐      ┌──────────────┐      ┌───────────┐
│  Client │      │ Auth Service │      │   Redis   │
└────┬────┘      └──────┬───────┘      └─────┬─────┘
     │                  │                    │
     │ 1. POST /refresh │                    │
     │ (refreshToken)   │                    │
     │─────────────────>│                    │
     │                  │                    │
     │                  │ 2. Validate refresh token
     │                  │───────────────────>│
     │                  │    (get userId)    │
     │                  │<───────────────────│
     │                  │                    │
     │                  │ 3. Get user credential
     │                  │─────────────┐      │
     │                  │<────────────┘      │
     │                  │                    │
     │                  │ 4. Check user status (active?)
     │                  │                    │
     │                  │ 5. Invalidate old refresh token
     │                  │───────────────────>│
     │                  │                    │
     │                  │ 6. Generate new access token
     │                  │ 7. Create new refresh token
     │                  │───────────────────>│
     │                  │                    │
     │ 8. Return new tokens                  │
     │<─────────────────│                    │
     │                  │                    │
```

---

### UC-04: Đăng Xuất (Logout)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `POST /api/v1/auth/logout` |
| **Actor** | Authenticated User |
| **Mục đích** | Vô hiệu hóa token và đăng xuất |

**Luồng hoạt động:**

```
┌─────────┐      ┌──────────────┐      ┌───────────┐
│  Client │      │ Auth Service │      │   Redis   │
└────┬────┘      └──────┬───────┘      └─────┬─────┘
     │                  │                    │
     │ 1. POST /logout  │                    │
     │ (accessToken,    │                    │
     │  refreshToken)   │                    │
     │─────────────────>│                    │
     │                  │                    │
     │                  │ 2. Parse access token
     │                  │    (get JTI, expiry)
     │                  │                    │
     │                  │ 3. Blacklist access token
     │                  │───────────────────>│
     │                  │    (TTL = remaining time)
     │                  │                    │
     │                  │ 4. Invalidate refresh token
     │                  │───────────────────>│
     │                  │                    │
     │ 5. Logout success│                    │
     │<─────────────────│                    │
     │                  │                    │
```

---

### UC-05: Xác Thực Email (Verify Email)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `GET /api/v1/auth/verify-email?token=xxx` |
| **Actor** | Guest (từ link trong email) |
| **Mục đích** | Xác thực địa chỉ email của người dùng |

**Luồng hoạt động:**

```
┌─────────┐      ┌──────────────┐      ┌───────────┐      ┌────────────┐
│  Email  │      │ Auth Service │      │   Redis   │      │ PostgreSQL │
└────┬────┘      └──────┬───────┘      └─────┬─────┘      └──────┬─────┘
     │                  │                    │                   │
     │ 1. Click verify link                  │                   │
     │─────────────────>│                    │                   │
     │                  │                    │                   │
     │                  │ 2. Validate token  │                   │
     │                  │───────────────────>│                   │
     │                  │    (get userId)    │                   │
     │                  │<───────────────────│                   │
     │                  │                    │                   │
     │                  │ 3. Get user credential                 │
     │                  │────────────────────────────────────────>│
     │                  │                    │                   │
     │                  │ 4. Update status:  │                   │
     │                  │    emailVerified=true                  │
     │                  │    status=active   │                   │
     │                  │────────────────────────────────────────>│
     │                  │                    │                   │
     │                  │ 5. Invalidate token│                   │
     │                  │───────────────────>│                   │
     │                  │                    │                   │
     │ 6. Email verified successfully        │                   │
     │<─────────────────│                    │                   │
     │                  │                    │                   │
```

---

### UC-06: Gửi Lại Email Xác Thực (Resend Verification)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `POST /api/v1/auth/resend-verification` |
| **Actor** | Guest |
| **Mục đích** | Gửi lại email xác thực nếu chưa nhận được |

**Rate Limiting:** Tối đa 3 lần trong 15 phút

---

### UC-07: Quên Mật Khẩu (Forgot Password)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `POST /api/v1/auth/forgot-password` |
| **Actor** | Guest |
| **Mục đích** | Yêu cầu đặt lại mật khẩu qua email |

**Luồng hoạt động:**

```
┌─────────┐      ┌──────────────┐      ┌───────────┐      ┌─────────────┐
│  Client │      │ Auth Service │      │   Redis   │      │ Email SMTP  │
└────┬────┘      └──────┬───────┘      └─────┬─────┘      └──────┬──────┘
     │                  │                    │                   │
     │ 1. POST /forgot-password              │                   │
     │ (email)          │                    │                   │
     │─────────────────>│                    │                   │
     │                  │                    │                   │
     │                  │ 2. Find user by email                  │
     │                  │ (không báo lỗi nếu không tìm thấy)     │
     │                  │                    │                   │
     │                  │ 3. Generate reset token                │
     │                  │───────────────────>│                   │
     │                  │ (store with TTL=15min)                 │
     │                  │                    │                   │
     │                  │ 4. Send reset email│                   │
     │                  │───────────────────────────────────────>│
     │                  │                    │                   │
     │ 5. "If account exists, email sent"    │                   │
     │<─────────────────│                    │                   │
     │                  │                    │                   │
```

**Bảo mật:** Luôn trả về message giống nhau để tránh lộ thông tin email tồn tại.

---

### UC-08: Đặt Lại Mật Khẩu (Reset Password)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `POST /api/v1/auth/reset-password` |
| **Actor** | Guest (từ link trong email) |
| **Mục đích** | Đặt mật khẩu mới |

**Request Body:**
```json
{
  "token": "reset_token_from_email",
  "newPassword": "NewPassword123!",
  "confirmPassword": "NewPassword123!"
}
```

**Luồng hoạt động:**

```
┌─────────┐      ┌──────────────┐      ┌───────────┐
│  Client │      │ Auth Service │      │   Redis   │
└────┬────┘      └──────┬───────┘      └─────┬─────┘
     │                  │                    │
     │ 1. POST /reset-password               │
     │─────────────────>│                    │
     │                  │                    │
     │                  │ 2. Validate passwords match
     │                  │                    │
     │                  │ 3. Validate token  │
     │                  │───────────────────>│
     │                  │    (get userId)    │
     │                  │<───────────────────│
     │                  │                    │
     │                  │ 4. Update password in DB
     │                  │                    │
     │                  │ 5. Invalidate reset token
     │                  │───────────────────>│
     │                  │                    │
     │                  │ 6. Invalidate ALL user tokens
     │                  │───────────────────>│
     │                  │ (logout tất cả thiết bị)
     │                  │                    │
     │ 7. Password reset success             │
     │<─────────────────│                    │
     │                  │                    │
```

---

### UC-09: Đổi Mật Khẩu (Change Password)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `POST /api/v1/auth/change-password` |
| **Actor** | Authenticated User |
| **Mục đích** | Thay đổi mật khẩu khi đang đăng nhập |
| **Auth** | Bearer Token required |

**Request Body:**
```json
{
  "currentPassword": "OldPassword123!",
  "newPassword": "NewPassword123!",
  "confirmPassword": "NewPassword123!"
}
```

**Validation:**
- Mật khẩu hiện tại phải đúng
- Mật khẩu mới phải khác mật khẩu cũ
- Mật khẩu mới và xác nhận phải khớp

---

### UC-10: Đăng Xuất Tất Cả Thiết Bị (Logout All Devices)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `POST /api/v1/auth/logout-all` |
| **Actor** | Authenticated User |
| **Mục đích** | Đăng xuất khỏi tất cả thiết bị |
| **Auth** | Bearer Token required |

**Luồng hoạt động:**
1. Lấy userId từ JWT token
2. Xóa tất cả refresh tokens của user trong Redis
3. Các access token vẫn còn hiệu lực đến khi hết hạn

---

### UC-11: Kiểm Tra Token (Introspect)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `POST /api/v1/auth/introspect` |
| **Actor** | Any Service |
| **Mục đích** | Kiểm tra tính hợp lệ của JWT token |

**Request Body:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Response:**
```json
{
  "code": 200,
  "result": {
    "valid": true
  }
}
```

---

## 2. User Service

> **Port:** 8083 | **Base Path:** `/api/v1/users`

### UC-12: Xem Profile Cá Nhân (Get My Profile)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `GET /api/v1/users/me` |
| **Actor** | Authenticated User |
| **Mục đích** | Xem thông tin profile cá nhân |
| **Auth** | Bearer Token required |

**Luồng hoạt động:**

```
┌─────────┐      ┌──────────────┐      ┌────────────┐      ┌───────────┐
│  Client │      │ User Service │      │ PostgreSQL │      │   MinIO   │
└────┬────┘      └──────┬───────┘      └──────┬─────┘      └─────┬─────┘
     │                  │                     │                  │
     │ 1. GET /users/me │                     │                  │
     │ (Bearer token)   │                     │                  │
     │─────────────────>│                     │                  │
     │                  │                     │                  │
     │                  │ 2. Extract userId from JWT             │
     │                  │                     │                  │
     │                  │ 3. Query profile with addresses       │
     │                  │────────────────────>│                  │
     │                  │                     │                  │
     │                  │ 4. Generate presigned URL for avatar  │
     │                  │────────────────────────────────────────>│
     │                  │                     │                  │
     │ 5. Return profile│                     │                  │
     │<─────────────────│                     │                  │
     │                  │                     │                  │
```

**Response:**
```json
{
  "code": 200,
  "result": {
    "userId": "uuid",
    "email": "user@example.com",
    "fullName": "Nguyễn Văn A",
    "phoneNumber": "0901234567",
    "avatarUrl": "https://presigned-url...",
    "dateOfBirth": "1990-01-15",
    "bio": "Hello world",
    "addresses": [...],
    "preferences": {
      "emailNotifications": true,
      "smsNotifications": false,
      "language": "vi",
      "currency": "VND",
      "theme": "light"
    }
  }
}
```

---

### UC-13: Cập Nhật Profile (Update My Profile)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `PUT /api/v1/users/me` |
| **Actor** | Authenticated User |
| **Mục đích** | Cập nhật thông tin cá nhân |
| **Auth** | Bearer Token required |

**Request Body:**
```json
{
  "fullName": "Nguyễn Văn B",
  "phoneNumber": "0909876543",
  "dateOfBirth": "1990-01-15",
  "bio": "Updated bio"
}
```

**Note:** Chỉ cập nhật các trường được gửi (partial update).

---

### UC-14: Xem Profile Người Khác (Get User by ID)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `GET /api/v1/users/{userId}` |
| **Actor** | Authenticated User |
| **Mục đích** | Xem thông tin công khai của user khác |
| **Auth** | Bearer Token required |

**Response (Limited Info):**
```json
{
  "code": 200,
  "result": {
    "userId": "uuid",
    "fullName": "Nguyễn Văn A",
    "avatarUrl": "https://..."
  }
}
```

---

### UC-15: Xem Danh Sách Địa Chỉ (Get My Addresses)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `GET /api/v1/users/me/addresses` |
| **Actor** | Authenticated User |
| **Mục đích** | Lấy danh sách địa chỉ giao hàng |
| **Auth** | Bearer Token required |

**Response:**
```json
{
  "code": 200,
  "result": [
    {
      "id": "uuid",
      "recipientName": "Nguyễn Văn A",
      "phone": "0901234567",
      "provinceName": "Hồ Chí Minh",
      "districtName": "Quận 1",
      "wardName": "Phường Bến Nghé",
      "streetAddress": "123 Đường ABC",
      "fullAddress": "123 Đường ABC, Phường Bến Nghé, Quận 1, Hồ Chí Minh",
      "addressType": "home",
      "isDefault": true,
      "label": "Nhà riêng"
    }
  ]
}
```

---

### UC-16: Thêm Địa Chỉ Mới (Create Address)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `POST /api/v1/users/me/addresses` |
| **Actor** | Authenticated User |
| **Mục đích** | Thêm địa chỉ giao hàng mới |
| **Auth** | Bearer Token required |

**Request Body:**
```json
{
  "recipientName": "Nguyễn Văn A",
  "phone": "0901234567",
  "provinceCode": "79",
  "provinceName": "Hồ Chí Minh",
  "districtCode": "760",
  "districtName": "Quận 1",
  "wardCode": "26734",
  "wardName": "Phường Bến Nghé",
  "streetAddress": "123 Đường ABC",
  "addressType": "home",
  "isDefault": true,
  "label": "Nhà riêng"
}
```

**Logic:**
- Nếu `isDefault = true` → Clear các địa chỉ default khác
- Nếu là địa chỉ đầu tiên → Tự động set `isDefault = true`

---

### UC-17: Cập Nhật Địa Chỉ (Update Address)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `PUT /api/v1/users/me/addresses/{addressId}` |
| **Actor** | Authenticated User |
| **Mục đích** | Chỉnh sửa địa chỉ giao hàng |
| **Auth** | Bearer Token required |

---

### UC-18: Xóa Địa Chỉ (Delete Address)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `DELETE /api/v1/users/me/addresses/{addressId}` |
| **Actor** | Authenticated User |
| **Mục đích** | Xóa địa chỉ giao hàng |
| **Auth** | Bearer Token required |

**Logic:**
- Nếu xóa địa chỉ default → Tự động set địa chỉ còn lại làm default

---

## 3. API Gateway

> **Port:** 8080

### UC-19: Routing & Load Balancing

| Thuộc tính | Giá trị |
|------------|---------|
| **Mục đích** | Định tuyến request đến các microservices |
| **Load Balancing** | Round-robin thông qua Consul |

**Route Configuration:**

| Path Pattern | Target Service | Rate Limit |
|--------------|----------------|------------|
| `/api/v1/auth/**` | auth-service | 5 req/sec (login), 10 req/sec (khác) |
| `/api/v1/users/**` | user-service | 10 req/sec |
| `/api/v1/products/**` | product-service | 10 req/sec |
| `/api/v1/orders/**` | order-service | 10 req/sec |

---

### UC-20: Rate Limiting

| Thuộc tính | Giá trị |
|------------|---------|
| **Mục đích** | Giới hạn số request để bảo vệ hệ thống |
| **Storage** | Redis |
| **Key** | Client IP Address |

**Luồng hoạt động:**

```
┌─────────┐      ┌─────────────┐      ┌───────────┐      ┌──────────────┐
│  Client │      │ API Gateway │      │   Redis   │      │   Service    │
└────┬────┘      └──────┬──────┘      └─────┬─────┘      └──────┬───────┘
     │                  │                   │                   │
     │ 1. Request       │                   │                   │
     │─────────────────>│                   │                   │
     │                  │                   │                   │
     │                  │ 2. Check rate limit│                  │
     │                  │──────────────────>│                   │
     │                  │                   │                   │
     │                  │ 3a. If allowed:   │                   │
     │                  │     Decrement bucket                  │
     │                  │     Forward request─────────────────>│
     │                  │                   │                   │
     │                  │ 3b. If exceeded:  │                   │
     │ HTTP 429         │     Return error  │                   │
     │<─────────────────│                   │                   │
     │                  │                   │                   │
```

**Response Headers:**
```
X-RateLimit-Remaining: 9
X-RateLimit-Replenish-Rate: 10
X-RateLimit-Burst-Capacity: 20
```

---

### UC-21: Swagger UI Aggregation

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `http://localhost:8080/swagger-ui.html` |
| **Mục đích** | Tổng hợp API docs từ tất cả services |

**Các API Docs được tổng hợp:**

| Service | Gateway Path | Source |
|---------|--------------|--------|
| Auth Service | `/api/v1/auth/v3/api-docs` | `auth-service:8088/api/v1/v3/api-docs` |
| User Service | `/api/v1/users/v3/api-docs` | `user-service:8083/api/v1/v3/api-docs` |
| Product Service | `/api/v1/products/v3/api-docs` | `product-service:8081/api/v1/v3/api-docs` |

---

## 4. Service-to-Service Communication

### UC-22: Service Token Authentication

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `POST /api/v1/auth/service/token` |
| **Actor** | Microservices (internal) |
| **Mục đích** | Services xác thực với nhau |

**Luồng hoạt động:**

```
┌──────────────┐      ┌──────────────┐      ┌───────────┐
│ User Service │      │ Auth Service │      │   Vault   │
└──────┬───────┘      └──────┬───────┘      └─────┬─────┘
       │                     │                    │
       │ 1. POST /service/token                   │
       │ (clientId, clientSecret)                 │
       │────────────────────>│                    │
       │                     │                    │
       │                     │ 2. Validate credentials
       │                     │───────────────────>│
       │                     │                    │
       │                     │ 3. Generate service token
       │                     │                    │
       │ 4. Return token     │                    │
       │<────────────────────│                    │
       │                     │                    │
```

**Request:**
```json
{
  "clientId": "user-service",
  "clientSecret": "secret-from-vault"
}
```

**Response:**
```json
{
  "code": 200,
  "result": {
    "accessToken": "service_token...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

---

### UC-23: Internal User Profile Creation

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `POST /api/v1/internal/users` |
| **Actor** | Auth Service |
| **Mục đích** | Tạo profile khi đăng ký tài khoản |
| **Auth** | Service Token (Role: SERVICE) |

**Luồng:** Auth Service → User Service khi đăng ký user mới.

---

### UC-24: Internal Get User Info

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `GET /api/v1/internal/users/{userId}/basic` |
| **Actor** | Other Services |
| **Mục đích** | Lấy thông tin user cho các service khác |
| **Auth** | Service Token (Role: SERVICE) |

**Response:**
```json
{
  "code": 200,
  "result": {
    "userId": "uuid",
    "email": "user@example.com",
    "fullName": "Nguyễn Văn A",
    "avatarUrl": "https://..."
  }
}
```

---

## 📊 Tổng Hợp Use Cases

| # | Use Case | Service | Endpoint | Auth Required |
|---|----------|---------|----------|---------------|
| 01 | Register | Auth | POST /auth/register | ❌ |
| 02 | Login | Auth | POST /auth/login | ❌ |
| 03 | Refresh Token | Auth | POST /auth/refresh | ❌ |
| 04 | Logout | Auth | POST /auth/logout | ❌ |
| 05 | Verify Email | Auth | GET /auth/verify-email | ❌ |
| 06 | Resend Verification | Auth | POST /auth/resend-verification | ❌ |
| 07 | Forgot Password | Auth | POST /auth/forgot-password | ❌ |
| 08 | Reset Password | Auth | POST /auth/reset-password | ❌ |
| 09 | Change Password | Auth | POST /auth/change-password | ✅ |
| 10 | Logout All | Auth | POST /auth/logout-all | ✅ |
| 11 | Introspect Token | Auth | POST /auth/introspect | ❌ |
| 12 | Get My Profile | User | GET /users/me | ✅ |
| 13 | Update My Profile | User | PUT /users/me | ✅ |
| 14 | Get User by ID | User | GET /users/{id} | ✅ |
| 15 | Get My Addresses | User | GET /users/me/addresses | ✅ |
| 16 | Create Address | User | POST /users/me/addresses | ✅ |
| 17 | Update Address | User | PUT /users/me/addresses/{id} | ✅ |
| 18 | Delete Address | User | DELETE /users/me/addresses/{id} | ✅ |
| 19 | Routing | Gateway | - | - |
| 20 | Rate Limiting | Gateway | - | - |
| 21 | Swagger Aggregation | Gateway | /swagger-ui.html | ❌ |
| 22 | Service Token | Auth | POST /auth/service/token | Service Credentials |
| 23 | Internal Create Profile | User | POST /internal/users | Service Token |
| 24 | Internal Get User | User | GET /internal/users/{id}/basic | Service Token |

---

## 🔐 JWT Token Structure

```json
{
  "sub": "user@example.com",
  "iss": "com.ecommerce",
  "jti": "unique-token-id",
  "iat": 1704067200,
  "exp": 1704070800,
  "userId": "uuid-string",
  "email": "user@example.com",
  "role": "customer",
  "scope": "customer"
}
```

**Token Expiration:**
- Access Token: 1 giờ (3600 giây)
- Refresh Token: 24 giờ (86400 giây)
- Verification Token: 24 giờ
- Password Reset Token: 15 phút

---

<p align="center">
  <b>📖 End of Use Cases Documentation</b>
</p>

