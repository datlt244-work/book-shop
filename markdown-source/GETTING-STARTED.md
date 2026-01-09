# 🚀 Hướng Dẫn Chạy Dự Án Book Shop từ A-Z

> Tài liệu này hướng dẫn chi tiết các bước từ clone source code đến chạy thành công toàn bộ hệ thống E-commerce Microservices.

---

## 📋 Mục Lục

1. [Yêu Cầu Hệ Thống](#1-yêu-cầu-hệ-thống)
2. [Clone Source Code](#2-clone-source-code)
3. [Khởi Tạo Infrastructure](#3-khởi-tạo-infrastructure)
4. [Build Dự Án](#4-build-dự-án)
5. [Chạy Các Services](#5-chạy-các-services)
6. [Kiểm Tra Hệ Thống](#6-kiểm-tra-hệ-thống)
7. [Sử Dụng API](#7-sử-dụng-api)
8. [Xử Lý Lỗi Thường Gặp](#8-xử-lý-lỗi-thường-gặp)
9. [Dừng Hệ Thống](#9-dừng-hệ-thống)

---

## 1. Yêu Cầu Hệ Thống

### 1.1 Phần Mềm Cần Cài Đặt

| Phần mềm | Phiên bản tối thiểu | Link tải |
|----------|---------------------|----------|
| **Docker Desktop** | Latest | [Download](https://www.docker.com/products/docker-desktop/) |
| **Java JDK** | 21+ (LTS) | [Download](https://adoptium.net/) hoặc [Oracle](https://www.oracle.com/java/technologies/downloads/) |
| **Apache Maven** | 3.9+ | [Download](https://maven.apache.org/download.cgi) |
| **Git** | Latest | [Download](https://git-scm.com/downloads) |
| **PowerShell** | 7+ (Windows) | Có sẵn trên Windows 10+ |

### 1.2 Kiểm Tra Cài Đặt

Mở PowerShell và chạy các lệnh sau để kiểm tra:

```powershell
# Kiểm tra Java
java -version
# Output mong đợi: openjdk version "21.x.x" hoặc cao hơn

# Kiểm tra Maven
mvn -version
# Output mong đợi: Apache Maven 3.9.x hoặc cao hơn

# Kiểm tra Docker
docker --version
docker-compose --version
# Đảm bảo Docker Desktop đang chạy

# Kiểm tra Git
git --version
```

### 1.3 Cấu Hình Tài Nguyên Docker

Mở Docker Desktop → Settings → Resources:
- **Memory**: Tối thiểu 6 GB (khuyến nghị 8 GB)
- **CPUs**: Tối thiểu 4 cores
- **Disk**: Tối thiểu 20 GB

---

## 2. Clone Source Code

### 2.1 Clone Repository

```powershell
# Clone repository
git clone https://github.com/your-org/book-shop.git

# Di chuyển vào thư mục dự án
cd book-shop
```

### 2.2 Cấu Trúc Thư Mục

```
book-shop/
├── api-gateway/              # API Gateway Service (Port: 8080)
├── auth-service/             # Authentication Service (Port: 8088)
├── common-lib/               # Shared Library
├── core-services/
│   ├── product-service/      # Product Service (Port: 8081)
│   └── user-service/         # User Service (Port: 8083)
├── support-services/
│   └── config-server/        # Config Server (Port: 8888)
├── infra/                    # Docker Compose & Scripts
│   ├── docker-compose.yml
│   ├── generate-env.ps1
│   ├── start-dev.ps1
│   ├── init-postgres/
│   ├── init-mongo/
│   └── vault/
├── pom.xml                   # Parent POM
└── README.md
```

---

## 3. Khởi Tạo Infrastructure

### 3.1 Phương Pháp 1: Quick Start (Khuyến Nghị)

```powershell
cd infra
.\start-dev.ps1
```

Script này sẽ tự động:
1. Tạo file `.env` với credentials ngẫu nhiên
2. Khởi động tất cả Docker containers
3. Chờ các services healthy
4. Khởi tạo Vault với secrets

### 3.2 Phương Pháp 2: Manual Setup (Từng Bước)

#### Bước 3.2.1: Tạo File Environment

```powershell
cd infra
.\generate-env.ps1
```

**Output mẫu:**
```
=== Generating .env file with random passwords ===

.env file created successfully!
Location: D:\KeHoach\book-shop\infra\.env

=== Generated Credentials ===
PostgreSQL: ecom_admin / dPjUZeI7g8Cg89bIpidptTA6
MongoDB:    ecom_admin / sRYibBELlV8AmywIxxsG1lZ5
Redis:      FWYe1CO3oyHIPv7AOVXQmA0s
MinIO:      ecom_admin / ij2v3sfOD57QAYWzcrBZsBXD
Vault:      wo8CQZT40x1fBWUldS4ube1GhkNz0OAc
```

> ⚠️ **QUAN TRỌNG**: Lưu lại các credentials này! File `.env` KHÔNG được commit lên Git.

#### Bước 3.2.2: Khởi Động Docker Containers

```powershell
docker-compose up -d
```

#### Bước 3.2.3: Kiểm Tra Trạng Thái Containers

```powershell
docker-compose ps
```

**Output mong đợi:**

| Container | Status |
|-----------|--------|
| ecom-postgres | Up (healthy) |
| ecom-mongo | Up (healthy) |
| ecom-redis | Up (healthy) |
| vault | Up (healthy) |
| consul | Up (healthy) |
| kafka | Up (healthy) |
| zookeeper | Up (healthy) |
| minio | Up (healthy) |

#### Bước 3.2.4: Chờ Containers Healthy

```powershell
# Chờ khoảng 30-60 giây để tất cả containers khởi động hoàn tất
# Kiểm tra lại
docker-compose ps
```

#### Bước 3.2.5: Khởi Tạo Vault

```powershell
.\vault\init-vault.ps1
```

**Output mong đợi:**
```
=== Initializing Vault for E-commerce System ===
Vault is ready!

Enabling KV secrets engine...
  KV engine enabled

Storing database credentials...
  PostgreSQL credentials stored (user: ecom_admin)
  MongoDB credentials stored (user: ecom_admin)
  Redis credentials stored

Storing service secrets...
  Auth service secrets stored
  Config server secrets stored
  API Gateway secrets stored
  Product service secrets stored

=============================================================================
Vault initialization complete!
=============================================================================
```

### 3.3 Cấu Hình JWT Signer Key (Bắt Buộc)

Bạn cần thêm `JWT_SIGNER_KEY` vào file `.env`:

```powershell
# Mở file .env
notepad infra\.env
```

Thêm dòng sau vào cuối file:

```bash
# JWT Configuration (CRITICAL - Must be at least 256 bits / 32 characters)
JWT_SIGNER_KEY=your-super-secret-jwt-signing-key-at-least-256-bits
```

> 💡 **Tip**: Có thể generate key bằng: `openssl rand -base64 32`

### 3.4 Infrastructure Endpoints

Sau khi khởi động, các services infrastructure có thể truy cập tại:

| Service | URL | Mô tả |
|---------|-----|-------|
| **Consul UI** | http://localhost:8500 | Service Discovery Dashboard |
| **Vault UI** | http://localhost:8200 | Secret Management |
| **MinIO Console** | http://localhost:9001 | Object Storage UI |
| **PostgreSQL** | localhost:5432 | Database |
| **MongoDB** | localhost:27017 | Document Database |
| **Redis** | localhost:6379 | Cache |
| **Kafka** | localhost:9092 | Message Broker |

---

## 4. Build Dự Án

### 4.1 Build Toàn Bộ Modules

```powershell
# Quay lại thư mục root của dự án
cd ..

# Build toàn bộ project (bỏ qua tests để tăng tốc)
mvn clean install -DskipTests
```

**Output mong đợi:**
```
[INFO] ecommerce-system ...................... SUCCESS
[INFO] common-lib ............................ SUCCESS
[INFO] api-gateway ........................... SUCCESS
[INFO] auth-service .......................... SUCCESS
[INFO] product-service ....................... SUCCESS
[INFO] user-service .......................... SUCCESS
[INFO] config-server ......................... SUCCESS
[INFO] ----------------------------------------
[INFO] BUILD SUCCESS
[INFO] ----------------------------------------
```

### 4.2 Build Module Riêng Lẻ (Optional)

```powershell
# Build common-lib trước (dependency cho các services khác)
cd common-lib
mvn clean install -DskipTests
cd ..

# Build từng service
mvn clean install -DskipTests -pl auth-service
mvn clean install -DskipTests -pl core-services/user-service
```

---

## 5. Chạy Các Services

### 5.1 Thứ Tự Khởi Động Services

> ⚠️ **QUAN TRỌNG**: Phải khởi động theo đúng thứ tự!

```
1. Infrastructure (Docker)  ✅ Đã hoàn thành ở Bước 3
2. Config Server            → Phải chạy đầu tiên
3. Auth Service             → Sau Config Server
4. User Service             → Sau Auth Service (hoặc song song)
5. Product Service          → Sau Auth Service (hoặc song song)
6. API Gateway              → Chạy cuối cùng
```

### 5.2 Mở Nhiều Terminal

Mỗi service cần chạy trong một terminal riêng. Mở 5 cửa sổ PowerShell mới.

### 5.3 Khởi Động Config Server (Terminal 1)

```powershell
cd D:\KeHoach\book-shop\support-services\config-server

# Set profile
$env:SPRING_PROFILES_ACTIVE = "native"

# Chạy service
mvn spring-boot:run
```

**Chờ cho đến khi thấy:**
```
Started ConfigServerApplication in X.XXX seconds
Tomcat started on port(s): 8888
```

**Kiểm tra Config Server:**
```powershell
# Trong terminal khác
curl http://localhost:8888/actuator/health
```

### 5.4 Khởi Động Auth Service (Terminal 2)

```powershell
cd D:\KeHoach\book-shop\auth-service

# Load biến môi trường từ .env file
Get-Content ..\infra\.env | ForEach-Object {
    if ($_ -match '^([^#][^=]*)=(.*)$') {
        [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2])
    }
}

# Set profile
$env:SPRING_PROFILES_ACTIVE = "dev"

# Chạy service
mvn spring-boot:run
```

**Chờ cho đến khi thấy:**
```
Started AuthServiceApplication in X.XXX seconds
Tomcat started on port(s): 8088
```

### 5.5 Khởi Động User Service (Terminal 3)

```powershell
cd D:\KeHoach\book-shop\core-services\user-service

# Load biến môi trường từ .env file
Get-Content ..\..\infra\.env | ForEach-Object {
    if ($_ -match '^([^#][^=]*)=(.*)$') {
        [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2])
    }
}

# Set profile
$env:SPRING_PROFILES_ACTIVE = "dev"

# Chạy service
mvn spring-boot:run
```

**Chờ cho đến khi thấy:**
```
Started UserServiceApplication in X.XXX seconds
Tomcat started on port(s): 8083
```

### 5.6 Khởi Động Product Service (Terminal 4)

```powershell
cd D:\KeHoach\book-shop\core-services\product-service

# Load biến môi trường từ .env file
Get-Content ..\..\infra\.env | ForEach-Object {
    if ($_ -match '^([^#][^=]*)=(.*)$') {
        [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2])
    }
}

# Set profile
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:VAULT_TOKEN = "your-vault-token-from-env-file"

# Chạy service
mvn spring-boot:run
```

### 5.7 Khởi Động API Gateway (Terminal 5)

```powershell
cd D:\KeHoach\book-shop\api-gateway

# Load biến môi trường từ .env file
Get-Content ..\infra\.env | ForEach-Object {
    if ($_ -match '^([^#][^=]*)=(.*)$') {
        [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2])
    }
}

# Set profile
$env:SPRING_PROFILES_ACTIVE = "dev"

# Chạy service
mvn spring-boot:run
```

**Chờ cho đến khi thấy:**
```
Started ApiGatewayApplication in X.XXX seconds
Netty started on port 8080
```

---

## 6. Kiểm Tra Hệ Thống

### 6.1 Kiểm Tra Health của Các Services

```powershell
# Config Server
curl http://localhost:8888/actuator/health

# Auth Service  
curl http://localhost:8088/api/v1/actuator/health

# User Service
curl http://localhost:8083/api/v1/actuator/health

# API Gateway
curl http://localhost:8080/actuator/health
```

**Output mong đợi cho mỗi service:**
```json
{
  "status": "UP"
}
```

### 6.2 Kiểm Tra Consul Service Discovery

Mở browser: http://localhost:8500

Các services đã đăng ký sẽ hiển thị:
- ✅ auth-service
- ✅ user-service
- ✅ product-service
- ✅ api-gateway
- ✅ config-server

### 6.3 Bảng Tổng Hợp Service Ports

| Service | Port | Health Check URL |
|---------|------|------------------|
| **API Gateway** | 8080 | http://localhost:8080/actuator/health |
| **Config Server** | 8888 | http://localhost:8888/actuator/health |
| **Auth Service** | 8088 | http://localhost:8088/api/v1/actuator/health |
| **User Service** | 8083 | http://localhost:8083/api/v1/actuator/health |
| **Product Service** | 8081 | http://localhost:8081/api/v1/actuator/health |

---

## 7. Sử Dụng API

### 7.1 Swagger UI (Khuyến Nghị)

Truy cập Swagger UI tổng hợp qua API Gateway:

**URL**: http://localhost:8080/swagger-ui.html

**Cách sử dụng:**
1. Mở URL trên
2. Chọn service trong dropdown **"Select a definition"**:
   - Auth Service - APIs đăng nhập, đăng ký
   - Product Service - APIs quản lý sản phẩm
   - User Service - APIs quản lý người dùng
3. Click **Explore** để load API documentation
4. Thử các API bằng cách click "Try it out"

### 7.2 Swagger UI Trực Tiếp Từng Service

| Service | Swagger UI URL |
|---------|----------------|
| Auth Service | http://localhost:8088/api/v1/swagger-ui.html |
| User Service | http://localhost:8083/api/v1/swagger-ui.html |
| Product Service | http://localhost:8081/api/v1/swagger-ui.html |

### 7.3 Test API Cơ Bản

#### Đăng Ký Tài Khoản

```powershell
curl -X POST http://localhost:8088/api/v1/auth/register `
  -H "Content-Type: application/json" `
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Password123!",
    "firstName": "Test",
    "lastName": "User"
  }'
```

#### Đăng Nhập

```powershell
curl -X POST http://localhost:8088/api/v1/auth/login `
  -H "Content-Type: application/json" `
  -d '{
    "username": "testuser",
    "password": "Password123!"
  }'
```

**Response mẫu:**
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

#### Sử Dụng Token để Gọi API Khác

```powershell
# Thay YOUR_ACCESS_TOKEN bằng token thực
curl -X GET http://localhost:8083/api/v1/users/profile `
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

## 8. Xử Lý Lỗi Thường Gặp

### 8.1 Lỗi: "Connection refused to localhost:8888"

**Nguyên nhân:** Config Server chưa khởi động.

**Giải pháp:**
```powershell
# Kiểm tra Config Server
curl http://localhost:8888/actuator/health

# Nếu không phản hồi, khởi động Config Server trước
cd support-services/config-server
mvn spring-boot:run
```

### 8.2 Lỗi: "Connection refused to Vault at localhost:8200"

**Nguyên nhân:** Vault container chưa chạy hoặc chưa healthy.

**Giải pháp:**
```powershell
cd infra

# Kiểm tra container
docker-compose ps vault

# Xem logs
docker-compose logs vault

# Restart nếu cần
docker-compose restart vault

# Chờ healthy rồi init lại
.\vault\init-vault.ps1
```

### 8.3 Lỗi: "Could not resolve placeholder 'jwt-signer-key'"

**Nguyên nhân:** JWT_SIGNER_KEY chưa được set trong environment.

**Giải pháp:**
```powershell
# Kiểm tra biến môi trường
echo $env:JWT_SIGNER_KEY

# Nếu rỗng, thêm vào file .env
notepad infra\.env
# Thêm dòng: JWT_SIGNER_KEY=your-super-secret-key-at-least-32-chars

# Load lại biến môi trường và restart service
```

### 8.4 Lỗi: "Connection to localhost:5432 refused"

**Nguyên nhân:** PostgreSQL container chưa chạy.

**Giải pháp:**
```powershell
cd infra

# Kiểm tra container
docker-compose ps postgres

# Xem logs
docker-compose logs postgres

# Kiểm tra credentials
Get-Content .env | Select-String "POSTGRES"
```

### 8.5 Lỗi: "HTTP 429 Too Many Requests"

**Nguyên nhân:** Rate limit exceeded.

**Giải pháp:**
- Chờ 1 giây và thử lại
- Kiểm tra Redis đang chạy: `docker-compose ps redis`

### 8.6 Lỗi Build: "Cannot resolve dependencies"

**Nguyên nhân:** common-lib chưa được build.

**Giải pháp:**
```powershell
# Build common-lib trước
cd common-lib
mvn clean install -DskipTests

# Sau đó build lại project
cd ..
mvn clean install -DskipTests
```

---

## 9. Dừng Hệ Thống

### 9.1 Dừng Các Spring Boot Services

Trong mỗi terminal đang chạy service, nhấn `Ctrl + C`.

### 9.2 Dừng Docker Containers

```powershell
cd infra

# Dừng tất cả containers (giữ data)
docker-compose stop

# Hoặc dừng và xóa containers (giữ data trong volumes)
docker-compose down

# Dừng và xóa toàn bộ (bao gồm data)
docker-compose down -v
```

### 9.3 Khởi Động Lại Hệ Thống

```powershell
cd infra

# Nếu đã có .env, chỉ cần
docker-compose up -d

# Chờ healthy
docker-compose ps

# Nếu cần init Vault lại (sau khi down -v)
.\vault\init-vault.ps1

# Sau đó khởi động các services theo thứ tự ở Bước 5
```

---

## 📚 Tài Liệu Tham Khảo

- [README.md](../README.md) - Tổng quan dự án
- [Environment-Setup-Guide.md](../markdown-source/Environment-Setup-Guide.md) - Hướng dẫn cấu hình môi trường chi tiết
- [HashiCorp-Vault-Setup-guide.md](../markdown-source/HashiCorp-Vault-Setup-guide.md) - Hướng dẫn Vault nâng cao

---

## ✅ Checklist Hoàn Thành

- [ ] Cài đặt Java 21+, Maven 3.9+, Docker Desktop, Git
- [ ] Clone repository thành công
- [ ] Tạo file `.env` với `generate-env.ps1`
- [ ] Thêm `JWT_SIGNER_KEY` vào `.env`
- [ ] Docker containers đang chạy và healthy
- [ ] Vault đã được initialize
- [ ] Build project thành công
- [ ] Config Server đang chạy (port 8888)
- [ ] Auth Service đang chạy (port 8088)
- [ ] User Service đang chạy (port 8083)
- [ ] API Gateway đang chạy (port 8080)
- [ ] Có thể truy cập Swagger UI
- [ ] Test đăng ký/đăng nhập thành công

---

<p align="center">
  <b>🎉 Chúc bạn chạy dự án thành công!</b>
</p>

<p align="center">
  Nếu gặp vấn đề, vui lòng liên hệ: datlt244@gmail.com
</p>

