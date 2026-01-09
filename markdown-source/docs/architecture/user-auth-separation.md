# User & Auth Service Separation

## Tổng quan

Hệ thống tách biệt **Authentication** (xác thực) và **User Profile** (thông tin người dùng) thành 2 microservices độc lập:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              API Gateway                                     │
│                           (port: 8080)                                       │
└─────────────────────┬───────────────────────────────┬───────────────────────┘
                      │                               │
                      ▼                               ▼
        ┌─────────────────────────┐     ┌─────────────────────────┐
        │      Auth Service       │     │      User Service       │
        │      (port: 8088)       │────▶│      (port: 8083)       │
        │                         │     │                         │
        │  • Login/Logout         │     │  • User Profile         │
        │  • Register             │     │  • Addresses            │
        │  • JWT Token            │     │  • Avatar               │
        │  • Password Reset       │     │  • Preferences          │
        │  • Email Verification   │     │                         │
        └───────────┬─────────────┘     └───────────┬─────────────┘
                    │                               │
                    ▼                               ▼
        ┌─────────────────────────┐     ┌─────────────────────────┐
        │    auth_service_db      │     │    user_service_db      │
        │   (user_credentials)    │     │   (user_profiles,       │
        │                         │     │    user_addresses)      │
        └─────────────────────────┘     └─────────────────────────┘
```

## Lý do tách biệt

| Aspect | Lợi ích |
|--------|---------|
| **Single Responsibility** | Mỗi service làm đúng một việc |
| **Independent Scaling** | Auth service có thể scale riêng khi traffic cao |
| **Security Isolation** | Credential data được bảo vệ riêng biệt |
| **Independent Deployment** | Update profile không ảnh hưởng authentication |
| **Database Isolation** | Mỗi service có database riêng |

---

## Auth Service

### Chức năng
- Đăng ký tài khoản (Register)
- Đăng nhập/Đăng xuất (Login/Logout)
- Quản lý JWT Token (Access Token, Refresh Token)
- Xác thực email (Email Verification)
- Đặt lại mật khẩu (Password Reset)
- Đổi mật khẩu (Change Password)

### Database Schema

```sql
-- Database: auth_service_db

CREATE TABLE user_credentials (
    id UUID PRIMARY KEY,                    -- Shared với user_profiles
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role user_role DEFAULT 'customer',      -- ENUM: customer, admin
    status user_status DEFAULT 'pending',   -- ENUM: pending_verification, active, inactive, blocked
    email_verified BOOLEAN DEFAULT FALSE,
    email_verified_at TIMESTAMP,
    last_login_at TIMESTAMP,
    last_login_ip VARCHAR(45),
    login_count INTEGER DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

### Entity

```java
@Entity
@Table(name = "user_credentials")
public class UserCredential {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private String email;
    private String passwordHash;
    
    @Enumerated(EnumType.STRING)
    private UserRole role;          // customer, admin
    
    @Enumerated(EnumType.STRING)
    private UserStatus status;      // pending_verification, active, inactive, blocked
    
    private Boolean emailVerified;
    private LocalDateTime emailVerifiedAt;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private Integer loginCount;
}
```

### API Endpoints

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| POST | `/api/v1/auth/register` | Đăng ký tài khoản mới | ❌ |
| POST | `/api/v1/auth/login` | Đăng nhập | ❌ |
| POST | `/api/v1/auth/logout` | Đăng xuất | ❌ |
| POST | `/api/v1/auth/refresh` | Refresh access token | ❌ |
| POST | `/api/v1/auth/introspect` | Kiểm tra token | ❌ |
| GET | `/api/v1/auth/verify-email` | Xác thực email | ❌ |
| POST | `/api/v1/auth/resend-verification` | Gửi lại email xác thực | ❌ |
| POST | `/api/v1/auth/forgot-password` | Quên mật khẩu | ❌ |
| POST | `/api/v1/auth/reset-password` | Đặt lại mật khẩu | ❌ |
| POST | `/api/v1/auth/change-password` | Đổi mật khẩu | ✅ |
| POST | `/api/v1/auth/logout-all` | Đăng xuất tất cả thiết bị | ✅ |

---

## User Service

### Chức năng
- Quản lý thông tin profile (tên, số điện thoại, ngày sinh, bio)
- Quản lý địa chỉ (nhiều địa chỉ, địa chỉ mặc định)
- Upload/Quản lý avatar
- Internal API cho service-to-service communication

### Database Schema

```sql
-- Database: user_service_db

CREATE TABLE user_profiles (
    user_id UUID PRIMARY KEY,               -- Shared với user_credentials
    email VARCHAR(255) UNIQUE NOT NULL,
    full_name VARCHAR(100),
    phone_number VARCHAR(20),
    avatar_url VARCHAR(500),
    date_of_birth DATE,
    bio TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE user_addresses (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES user_profiles(user_id),
    label VARCHAR(50),                      -- "Home", "Office", etc.
    recipient_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    street_address VARCHAR(255) NOT NULL,
    ward VARCHAR(100),
    district VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(100) DEFAULT 'Vietnam',
    postal_code VARCHAR(20),
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

### Entities

```java
@Entity
@Table(name = "user_profiles")
public class UserProfile {
    @Id
    private UUID userId;        // Không auto-generate, nhận từ auth-service
    
    private String email;
    private String fullName;
    private String phoneNumber;
    private String avatarUrl;
    private LocalDate dateOfBirth;
    private String bio;
    
    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL)
    private List<UserAddress> addresses;
}

@Entity
@Table(name = "user_addresses")
public class UserAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserProfile userProfile;
    
    private String label;
    private String recipientName;
    private String phoneNumber;
    private String streetAddress;
    private String ward;
    private String district;
    private String city;
    private String country;
    private String postalCode;
    private Boolean isDefault;
}
```

### API Endpoints

#### Public APIs (cho User)

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| GET | `/api/v1/users/me` | Lấy profile hiện tại | ✅ |
| PUT | `/api/v1/users/me` | Cập nhật profile | ✅ |
| POST | `/api/v1/users/me/avatar` | Upload avatar | ✅ |
| DELETE | `/api/v1/users/me/avatar` | Xóa avatar | ✅ |
| GET | `/api/v1/users/me/addresses` | Danh sách địa chỉ | ✅ |
| POST | `/api/v1/users/me/addresses` | Thêm địa chỉ mới | ✅ |
| PUT | `/api/v1/users/me/addresses/{id}` | Cập nhật địa chỉ | ✅ |
| DELETE | `/api/v1/users/me/addresses/{id}` | Xóa địa chỉ | ✅ |
| PUT | `/api/v1/users/me/addresses/{id}/default` | Đặt địa chỉ mặc định | ✅ |

#### Internal APIs (cho Service-to-Service)

| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| POST | `/api/v1/internal/users` | Tạo profile mới | Service Token |
| GET | `/api/v1/internal/users/{userId}` | Lấy profile theo ID | Service Token |
| GET | `/api/v1/internal/users/{userId}/basic` | Lấy thông tin cơ bản | Service Token |
| DELETE | `/api/v1/internal/users/{userId}` | Xóa profile | Service Token |
| GET | `/api/v1/internal/users/{userId}/exists` | Kiểm tra tồn tại | Service Token |

---

## Luồng hoạt động

### 1. Đăng ký tài khoản (Register)

```
┌──────┐     ┌─────────────┐     ┌──────────────┐     ┌──────────────┐
│Client│     │ API Gateway │     │ Auth Service │     │ User Service │
└──┬───┘     └──────┬──────┘     └──────┬───────┘     └──────┬───────┘
   │                │                   │                    │
   │  POST /auth/register               │                    │
   │  {email, password, fullName}       │                    │
   │───────────────▶│                   │                    │
   │                │───────────────────▶                    │
   │                │                   │                    │
   │                │    1. Validate & Create UserCredential │
   │                │    2. Generate UUID                    │
   │                │    3. Hash password                    │
   │                │    4. Save to auth_service_db          │
   │                │                   │                    │
   │                │                   │  POST /internal/users
   │                │                   │  {userId, email, fullName}
   │                │                   │───────────────────▶│
   │                │                   │                    │
   │                │                   │    Create UserProfile
   │                │                   │    with same UUID
   │                │                   │                    │
   │                │                   │◀───────────────────│
   │                │                   │                    │
   │                │    5. Send verification email          │
   │                │◀──────────────────│                    │
   │◀───────────────│                   │                    │
   │                │                   │                    │
   │  Response: {userId, email, status: "pending"}           │
   │                │                   │                    │
```

### 2. Đăng nhập (Login)

```
┌──────┐     ┌─────────────┐     ┌──────────────┐     ┌──────────────┐
│Client│     │ API Gateway │     │ Auth Service │     │ User Service │
└──┬───┘     └──────┬──────┘     └──────┬───────┘     └──────┬───────┘
   │                │                   │                    │
   │  POST /auth/login                  │                    │
   │  {email, password}                 │                    │
   │───────────────▶│                   │                    │
   │                │───────────────────▶                    │
   │                │                   │                    │
   │                │    1. Find UserCredential by email     │
   │                │    2. Verify password                  │
   │                │    3. Check status (must be active)    │
   │                │    4. Update login tracking            │
   │                │                   │                    │
   │                │                   │  GET /internal/users/{id}/basic
   │                │                   │───────────────────▶│
   │                │                   │                    │
   │                │                   │  {fullName, avatarUrl}
   │                │                   │◀───────────────────│
   │                │                   │                    │
   │                │    5. Generate JWT (access + refresh)  │
   │                │◀──────────────────│                    │
   │◀───────────────│                   │                    │
   │                │                   │                    │
   │  Response: {accessToken, refreshToken, userId,          │
   │             email, fullName, avatarUrl, role}           │
   │                │                   │                    │
```

### 3. Lấy Profile

```
┌──────┐     ┌─────────────┐     ┌──────────────┐
│Client│     │ API Gateway │     │ User Service │
└──┬───┘     └──────┬──────┘     └──────┬───────┘
   │                │                   │
   │  GET /users/me                     │
   │  Authorization: Bearer <token>     │
   │───────────────▶│                   │
   │                │                   │
   │                │  Validate JWT     │
   │                │  Extract userId   │
   │                │                   │
   │                │───────────────────▶
   │                │                   │
   │                │    Find UserProfile by userId
   │                │    Generate avatar presigned URL
   │                │                   │
   │                │◀──────────────────│
   │◀───────────────│                   │
   │                │                   │
   │  Response: {userId, email, fullName, phoneNumber,
   │             avatarUrl, dateOfBirth, bio, addresses}
   │                │                   │
```

---

## Service-to-Service Communication

### Feign Client (Auth → User)

```java
@FeignClient(
    name = "user-service",
    path = "/api/v1/internal",
    fallbackFactory = UserServiceClientFallback.class
)
public interface UserServiceClient {

    @PostMapping("/users")
    ApiResponse<UserProfileInfo> createUserProfile(@RequestBody CreateUserProfileRequest request);

    @GetMapping("/users/{userId}/basic")
    ApiResponse<UserBasicInfo> getUserBasicInfo(@PathVariable("userId") UUID userId);

    @GetMapping("/users/{userId}")
    ApiResponse<UserProfileInfo> getUserProfile(@PathVariable("userId") UUID userId);

    @DeleteMapping("/users/{userId}")
    ApiResponse<Void> deleteUserProfile(@PathVariable("userId") UUID userId);
}
```

### Authentication Flow (Service Token)

```
┌──────────────┐                    ┌──────────────┐
│ Auth Service │                    │ User Service │
└──────┬───────┘                    └──────┬───────┘
       │                                   │
       │  1. Get Service Token (cached)    │
       │     POST /auth/service/token      │
       │     {clientId, clientSecret}      │
       │                                   │
       │  2. Call Internal API             │
       │     Authorization: Bearer <service-token>
       │     X-Service-Name: auth-service  │
       │─────────────────────────────────▶│
       │                                   │
       │     3. Validate Service Token     │
       │     4. Process Request            │
       │                                   │
       │◀─────────────────────────────────│
       │                                   │
```

---

## Cấu trúc thư mục

```
book-shop/
├── auth-service/
│   └── src/main/java/com/ecommerce/auth_service/
│       ├── client/
│       │   ├── UserServiceClient.java          # Feign client
│       │   ├── UserServiceClientFallback.java  # Fallback handler
│       │   └── dto/
│       │       ├── CreateUserProfileRequest.java
│       │       ├── UserBasicInfo.java
│       │       └── UserProfileInfo.java
│       ├── controller/
│       │   └── AuthController.java
│       ├── entity/
│       │   ├── UserCredential.java
│       │   ├── UserRole.java
│       │   └── UserStatus.java
│       ├── repository/
│       │   └── UserCredentialRepository.java
│       └── service/
│           ├── AuthenticationService.java
│           ├── TokenRedisService.java
│           ├── EmailVerificationService.java
│           └── PasswordResetService.java
│
└── core-services/user-service/
    └── src/main/java/com/ecommerce/user/
        ├── controller/
        │   ├── UserController.java             # Public API
        │   └── UserInternalController.java     # Internal API
        ├── entity/
        │   ├── UserProfile.java
        │   └── UserAddress.java
        ├── repository/
        │   ├── UserProfileRepository.java
        │   └── UserAddressRepository.java
        ├── service/
        │   └── UserService.java
        └── dto/
            ├── request/
            │   ├── CreateUserProfileRequest.java
            │   ├── UpdateProfileRequest.java
            │   ├── CreateAddressRequest.java
            │   └── UpdateAddressRequest.java
            └── response/
                ├── UserProfileResponse.java
                ├── UserBasicInfoResponse.java
                └── AddressResponse.java
```

---

## Shared UUID Strategy

Auth Service và User Service chia sẻ cùng một `UUID` cho user:

```
┌─────────────────────────────────────────────────────────────┐
│                        UUID: abc-123-...                     │
├─────────────────────────────┬───────────────────────────────┤
│      auth_service_db        │       user_service_db         │
│                             │                               │
│  user_credentials           │  user_profiles                │
│  ┌─────────────────────┐    │  ┌─────────────────────┐      │
│  │ id: abc-123-...     │◀───┼──│ user_id: abc-123-...│      │
│  │ email: user@mail.com│    │  │ email: user@mail.com│      │
│  │ password_hash: ...  │    │  │ full_name: John     │      │
│  │ role: customer      │    │  │ phone: 0123456789   │      │
│  │ status: active      │    │  │ avatar_url: ...     │      │
│  └─────────────────────┘    │  └─────────────────────┘      │
└─────────────────────────────┴───────────────────────────────┘
```

**Lưu ý quan trọng:**
- UUID được **generate bởi Auth Service** khi register
- User Service **nhận UUID** từ Auth Service và dùng làm Primary Key
- Đảm bảo **data consistency** giữa 2 services

---

## Error Handling

### Auth Service Errors

| Error Code | Message | HTTP Status |
|------------|---------|-------------|
| `USER_EXISTED` | Email đã tồn tại | 400 |
| `UNAUTHENTICATED` | Sai email hoặc mật khẩu | 401 |
| `EMAIL_NOT_VERIFIED` | Email chưa được xác thực | 403 |
| `ACCOUNT_BLOCKED` | Tài khoản bị khóa | 403 |
| `TOKEN_INVALID` | Token không hợp lệ | 401 |
| `RATE_LIMITED` | Quá nhiều request | 429 |

### User Service Errors

| Error Code | Message | HTTP Status |
|------------|---------|-------------|
| `USER_NOT_FOUND` | Không tìm thấy user | 404 |
| `ADDRESS_NOT_FOUND` | Không tìm thấy địa chỉ | 404 |
| `MAX_ADDRESSES_REACHED` | Đã đạt giới hạn địa chỉ | 400 |
| `INVALID_FILE_TYPE` | File không hợp lệ | 400 |

---

## Configuration

### Auth Service (`application.yaml`)

```yaml
server:
  port: 8088
  servlet:
    context-path: /api/v1

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/auth_service_db

# Feign Client Config
spring.cloud.openfeign:
  client:
    config:
      user-service:
        connect-timeout: 5000
        read-timeout: 5000
```

### User Service (`application.yaml`)

```yaml
server:
  port: 8083
  servlet:
    context-path: /api/v1

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/user_service_db

# Service Authentication
service:
  auth:
    enabled: true
    client-id: user-service
    client-secret: ${client-secret}
    auth-service-url: http://auth-service:8088/api/v1
```

---

## Testing

### Test Register Flow

```bash
# 1. Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test@123",
    "fullName": "Test User",
    "phoneNumber": "0123456789"
  }'

# 2. Verify Email (click link in email or use token directly)
curl http://localhost:8080/api/v1/auth/verify-email?token=<token>

# 3. Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test@123"
  }'

# 4. Get Profile
curl http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer <access_token>"
```

---

## Monitoring & Health Check

### Endpoints

| Service | Health Check URL |
|---------|------------------|
| Auth Service | `http://localhost:8088/api/v1/actuator/health` |
| User Service | `http://localhost:8083/api/v1/actuator/health` |

### Metrics

Cả 2 services expose metrics qua Actuator:
- `/actuator/metrics` - All metrics
- `/actuator/metrics/http.server.requests` - HTTP request metrics
- `/actuator/metrics/jvm.memory.used` - JVM memory usage

