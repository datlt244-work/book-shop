# 📚 Book Shop - E-commerce Microservices System

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java" alt="Java 21"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0.0-brightgreen?style=for-the-badge&logo=spring-boot" alt="Spring Boot 4.0.0"/>
  <img src="https://img.shields.io/badge/Spring%20Cloud-2025.1.0-brightgreen?style=for-the-badge&logo=spring" alt="Spring Cloud 2025.1.0"/>
  <img src="https://img.shields.io/badge/Docker-Compose-blue?style=for-the-badge&logo=docker" alt="Docker"/>
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge" alt="License"/>
</p>

---

## 📋 Mục lục

- [Tổng quan](#-tổng-quan)
- [Quick Start](#-quick-start)
- [Kiến trúc hệ thống](#-kiến-trúc-hệ-thống)
- [Công nghệ sử dụng](#-công-nghệ-sử-dụng)
- [Cấu trúc dự án](#-cấu-trúc-dự-án)
- [Các Services](#-các-services)
- [Yêu cầu hệ thống](#-yêu-cầu-hệ-thống)
- [Hướng dẫn cài đặt](#-hướng-dẫn-cài-đặt)
- [Chạy ứng dụng](#-chạy-ứng-dụng)
- [API Documentation](#-api-documentation)
- [Quản lý Secrets với Vault](#-quản-lý-secrets-với-vault)
- [Environment Profiles](#-environment-profiles)
- [Troubleshooting](#-troubleshooting)
- [Đóng góp](#-đóng-góp)
- [License](#-license)

> 📖 **Hướng dẫn chi tiết từ A-Z:** Xem [GETTING-STARTED.md](markdown-source/GETTING-STARTED.md) để có hướng dẫn đầy đủ từ clone code đến chạy thành công.

---

## 🎯 Tổng quan

**Book Shop** là một hệ thống E-commerce được xây dựng theo kiến trúc **Microservices** hiện đại, sử dụng các công nghệ mới nhất của Spring ecosystem. Dự án được thiết kế để có khả năng mở rộng cao, bảo mật tốt và dễ dàng triển khai trên nhiều môi trường khác nhau.

### ✨ Tính năng chính

- 🔐 **Authentication & Authorization** - Hệ thống xác thực người dùng với JWT và OAuth2
- 📦 **Product Management** - Quản lý sản phẩm, danh mục, và kho hàng
- 🛒 **Order Management** - Xử lý đơn hàng và thanh toán
- 👤 **User Management** - Quản lý thông tin người dùng
- 🔍 **Service Discovery** - Tự động phát hiện và đăng ký services với Consul
- 🔒 **Secret Management** - Quản lý bí mật an toàn với HashiCorp Vault
- ⚡ **Rate Limiting** - Giới hạn request với Redis
- 📊 **Health Monitoring** - Giám sát sức khỏe hệ thống với Actuator
- 📝 **API Documentation** - Tự động sinh tài liệu API với OpenAPI/Swagger

---

## ⚡ Quick Start

> Hướng dẫn nhanh để chạy dự án. Xem [GETTING-STARTED.md](markdown-source/GETTING-STARTED.md) để có hướng dẫn chi tiết hơn.

### Prerequisites
- Docker Desktop (đang chạy)
- Java 21+
- Maven 3.9+
- Git

### 1. Clone & Setup Infrastructure

```powershell
# Clone repository
git clone https://github.com/your-org/book-shop.git
cd book-shop

# Khởi động infrastructure (tự động tạo .env và init Vault)
cd infra
.\start-dev.ps1
```

### 2. Thêm JWT Key vào .env

```powershell
# Mở file .env và thêm dòng sau:
notepad .env
# JWT_SIGNER_KEY=your-super-secret-jwt-key-at-least-32-characters
```

### 3. Build & Run Services

```powershell
# Build project
cd ..
mvn clean install -DskipTests

# Terminal 1: Config Server
cd support-services/config-server
$env:SPRING_PROFILES_ACTIVE = "native"
mvn spring-boot:run

# Terminal 2: Auth Service (load .env trước)
cd auth-service
Get-Content ..\infra\.env | ForEach-Object { if ($_ -match '^([^#][^=]*)=(.*)$') { [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2]) } }
$env:SPRING_PROFILES_ACTIVE = "dev"
mvn spring-boot:run

# Terminal 3: API Gateway
cd api-gateway
Get-Content ..\infra\.env | ForEach-Object { if ($_ -match '^([^#][^=]*)=(.*)$') { [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2]) } }
$env:SPRING_PROFILES_ACTIVE = "dev"
mvn spring-boot:run
```

### 4. Truy cập

| Service | URL |
|---------|-----|
| **Swagger UI** | http://localhost:8080/swagger-ui.html |
| **API Gateway** | http://localhost:8080 |
| **Consul UI** | http://localhost:8500 |
| **Vault UI** | http://localhost:8200 |

---

## 🏗 Kiến trúc hệ thống

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                   CLIENT                                        │
│                        (Web Browser / Mobile App)                               │
└───────────────────────────────────┬─────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              API GATEWAY                                        │
│                           (Spring Cloud Gateway)                                │
│                                                                                 │
│  • Routing & Load Balancing          • Rate Limiting (Redis)                    │
│  • JWT Token Validation              • Request/Response Logging                 │
│  • CORS Configuration                • Swagger UI Aggregation                   │
└───────────────────────────────────┬─────────────────────────────────────────────┘
                                    │
          ┌─────────────────────────┼─────────────────────────┐
          │                         │                         │
          ▼                         ▼                         ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   AUTH SERVICE  │     │ PRODUCT SERVICE │     │  ORDER SERVICE  │
│   (Port: 8088)  │     │   (Port: 8081)  │     │   (Port: 8082)  │
│                 │     │                 │     │                 │
│ • User Auth     │     │ • Products CRUD │     │ • Order Process │
│ • JWT Tokens    │     │ • Categories    │     │ • Payment       │
│ • OAuth2        │     │ • Inventory     │     │ • Status Track  │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   PostgreSQL    │     │     MongoDB     │     │   PostgreSQL    │
│  (Auth Data)    │     │ (Product Data)  │     │ (Order Data)    │
└─────────────────┘     └─────────────────┘     └─────────────────┘

                    SUPPORT SERVICES
          ┌─────────────────────────────────────┐
          │                                     │
┌─────────┴─────────┐     ┌─────────────────────┴───────────────────┐
│   CONFIG SERVER   │     │              INFRASTRUCTURE             │
│   (Port: 8888)    │     │                                         │
│                   │     │  • Consul (Service Discovery) :8500     │
│ • Centralized     │     │  • Vault (Secret Management) :8200      │
│   Configuration   │     │  • Redis (Cache & Rate Limit) :6379     │
│ • Environment     │     │  • Kafka (Message Broker) :9092         │
│   Profiles        │     │  • MinIO (Object Storage) :9000         │
└───────────────────┘     └─────────────────────────────────────────┘
```

### 🔄 Config Loading Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Config Loading Flow                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   ┌──────────────┐      ┌──────────────┐      ┌──────────────┐      │
│   │   Service    │ ──▶  │ Config Server│ ──▶  │    Vault     │     │
│   │ application  │      │  (Port 8888) │      │ (Port 8200)  │      │
│   │    .yaml     │      │              │      │              │      │
│   └──────────────┘      └──────────────┘      └──────────────┘      │
│         │                      │                     │              │
│         ▼                      ▼                     ▼              │
│   SPRING_PROFILES       Profile-specific        Secrets             │
│   _ACTIVE=dev           configurations         (JWT, DB pass)       │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🛠 Công nghệ sử dụng

### Backend Framework
| Công nghệ | Phiên bản | Mô tả |
|-----------|-----------|-------|
| **Java** | 21 (LTS) | Ngôn ngữ lập trình chính |
| **Spring Boot** | 4.0.0 | Framework phát triển ứng dụng |
| **Spring Cloud** | 2025.1.0 | Cloud-native microservices |
| **Spring Security** | - | Bảo mật ứng dụng với OAuth2/JWT |
| **Spring Data JPA** | - | ORM cho PostgreSQL |
| **Spring Data MongoDB** | - | ODM cho MongoDB |
| **Spring Data Redis** | - | Cache và Rate Limiting |

### Infrastructure & DevOps
| Công nghệ | Phiên bản | Mô tả |
|-----------|-----------|-------|
| **Docker** | - | Container hóa ứng dụng |
| **Docker Compose** | - | Orchestration cho development |
| **PostgreSQL** | 15 | Database cho Auth, Order, User |
| **MongoDB** | 6.0 | Database cho Product, Review |
| **Redis** | 7 | Cache, Session, Rate Limiting |
| **Apache Kafka** | 7.5.0 | Message Broker |
| **HashiCorp Consul** | - | Service Discovery |
| **HashiCorp Vault** | 1.15 | Secret Management |
| **MinIO** | - | Object Storage (S3-compatible) |

### Documentation & Testing
| Công nghệ | Phiên bản | Mô tả |
|-----------|-----------|-------|
| **SpringDoc OpenAPI** | 2.8.8 | API Documentation |
| **Swagger UI** | - | Interactive API Explorer |
| **Spring Boot Test** | - | Unit & Integration Testing |

---

## 📁 Cấu trúc dự án

```
book-shop/
├── 📁 api-gateway/              # API Gateway Service
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   └── test/
│   └── pom.xml
│
├── 📁 auth-service/             # Authentication Service
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/ecommerce/auth/
│   │   │   │       ├── config/
│   │   │   │       ├── controller/
│   │   │   │       ├── dto/
│   │   │   │       ├── entity/
│   │   │   │       ├── exception/
│   │   │   │       ├── repository/
│   │   │   │       └── service/
│   │   │   └── resources/
│   │   └── test/
│   └── pom.xml
│
├── 📁 common-lib/               # Shared Library
│   ├── src/
│   └── pom.xml
│
├── 📁 core-services/            # Core Business Services
│   ├── 📁 product-service/      # Product Management Service (Port: 8081)
│   │   ├── src/
│   │   └── pom.xml
│   └── 📁 user-service/         # User Profile Service (Port: 8083)
│       ├── src/
│       └── pom.xml
│
├── 📁 support-services/         # Support Services
│   └── 📁 config-server/        # Centralized Configuration
│       ├── src/
│       └── pom.xml
│
├── 📁 infra/                    # Infrastructure
│   ├── docker-compose.yml       # Development environment
│   ├── docker-compose.staging.yml
│   ├── docker-compose.prod.yml
│   ├── docker-compose.override.yml
│   ├── generate-env.ps1         # Generate .env file
│   ├── start-dev.ps1            # Start development
│   ├── start-staging.ps1        # Start staging
│   ├── start-prod.ps1           # Start production
│   ├── 📁 init-postgres/        # PostgreSQL init scripts
│   ├── 📁 init-mongo/           # MongoDB init scripts
│   └── 📁 vault/                # Vault configuration
│       ├── init-vault.ps1       # Initialize Vault
│       ├── config/
│       └── policies/
│
├── 📁 markdown-source/          # Documentation sources
│   ├── GETTING-STARTED.md       # 🚀 Hướng dẫn chạy dự án từ A-Z
│   ├── USE-CASES.md             # 📋 Danh sách Use Cases & luồng hoạt động
│   ├── Environment-Setup-Guide.md
│   └── HashiCorp-Vault-Setup-guide.md
│
├── pom.xml                      # Parent POM
├── .gitignore
└── README.md
```

---

## 🔧 Các Services

### 1. API Gateway (Port: 8080)
**Vai trò:** Entry point cho tất cả requests từ client

| Feature | Mô tả |
|---------|-------|
| **Routing** | Định tuyến requests đến các microservices |
| **Load Balancing** | Cân bằng tải giữa các instances |
| **Rate Limiting** | Giới hạn số request/giây |
| **JWT Validation** | Xác thực token trước khi forward |
| **Swagger Aggregation** | Tổng hợp API docs từ tất cả services |

### 2. Auth Service (Port: 8088)
**Vai trò:** Xác thực và phân quyền người dùng

| Feature | Mô tả |
|---------|-------|
| **User Registration** | Đăng ký tài khoản mới |
| **Login/Logout** | Đăng nhập/đăng xuất |
| **JWT Tokens** | Cấp phát và quản lý tokens |
| **OAuth2** | Hỗ trợ OAuth2 Resource Server |
| **Email Verification** | Xác thực email người dùng |

### 3. Product Service (Port: 8081)
**Vai trò:** Quản lý sản phẩm

| Feature | Mô tả |
|---------|-------|
| **Product CRUD** | Thêm, sửa, xóa, xem sản phẩm |
| **Categories** | Quản lý danh mục sản phẩm |
| **Search** | Tìm kiếm sản phẩm |
| **Image Upload** | Upload hình ảnh sản phẩm (MinIO) |

### 4. User Service (Port: 8083)
**Vai trò:** Quản lý thông tin người dùng

| Feature | Mô tả |
|---------|-------|
| **User Profile** | Quản lý hồ sơ người dùng |
| **Addresses** | Quản lý địa chỉ giao hàng |
| **Preferences** | Cài đặt tùy chọn người dùng |
| **Avatar Upload** | Upload ảnh đại diện (MinIO) |

### 5. Config Server (Port: 8888)
**Vai trò:** Quản lý cấu hình tập trung

| Feature | Mô tả |
|---------|-------|
| **Centralized Config** | Lưu trữ config cho tất cả services |
| **Profile-based** | Cấu hình theo môi trường (dev/staging/prod) |
| **Vault Integration** | Tích hợp với Vault cho secrets |

---

## 💻 Yêu cầu hệ thống

### Prerequisites

| Yêu cầu | Phiên bản tối thiểu |
|---------|---------------------|
| **Docker Desktop** | Latest |
| **Java JDK** | 21+ |
| **Maven** | 3.9+ |
| **PowerShell** | 7+ (Windows) |
| **Git** | Latest |

### Hardware khuyến nghị

| Tài nguyên | Development | Staging/Production |
|------------|-------------|-------------------|
| **RAM** | 8 GB | 16 GB+ |
| **CPU** | 4 cores | 8 cores+ |
| **Disk** | 20 GB SSD | 50 GB+ SSD |

---

## 🚀 Hướng dẫn cài đặt

### Bước 1: Clone Repository

```powershell
git clone https://github.com/your-org/book-shop.git
cd book-shop
```

### Bước 2: Tạo file .env

```powershell
cd infra
.\generate-env.ps1
```

Sau khi chạy xong, bạn sẽ thấy output như sau:

```
=== Generating .env file with random passwords ===

.env file created successfully!

=== Generated Credentials ===
PostgreSQL: ecom_admin / dPjUZeI7g8Cg89bIpidptTA6
MongoDB:    ecom_admin / sRYibBELlV8AmywIxxsG1lZ5
Redis:      FWYe1CO3oyHIPv7AOVXQmA0s
MinIO:      ecom_admin / ij2v3sfOD57QAYWzcrBZsBXD
Vault:      wo8CQZT40x1fBWUldS4ube1GhkNz0OAc
```

> ⚠️ **Quan trọng:** File `.env` chứa thông tin nhạy cảm và đã được thêm vào `.gitignore`. **KHÔNG commit file này lên repository!**

### Bước 3: Khởi động Infrastructure

```powershell
docker-compose up -d
```

Kiểm tra trạng thái:
```powershell
docker-compose ps
```

### Bước 4: Khởi tạo Vault

```powershell
.\vault\init-vault.ps1
```

### Bước 5: Build dự án

```powershell
cd ..
mvn clean install
```

---

## ▶️ Chạy ứng dụng

### Quick Start (Development)

```powershell
cd infra
.\start-dev.ps1
```

### Manual Start (Step by Step)

#### 1. Start Config Server
```powershell
cd support-services/config-server
$env:SPRING_PROFILES_ACTIVE = "native"
mvn spring-boot:run
```

#### 2. Start Auth Service
```powershell
cd auth-service
$env:SPRING_PROFILES_ACTIVE = "dev"
mvn spring-boot:run
```

#### 3. Start User Service
```powershell
cd core-services/user-service
$env:SPRING_PROFILES_ACTIVE = "dev"
mvn spring-boot:run
```

#### 4. Start Product Service
```powershell
cd core-services/product-service
$env:SPRING_PROFILES_ACTIVE = "dev"
mvn spring-boot:run
```

#### 5. Start API Gateway
```powershell
cd api-gateway
$env:SPRING_PROFILES_ACTIVE = "dev"
mvn spring-boot:run
```

### Development URLs

| Service | URL |
|---------|-----|
| **API Gateway** | http://localhost:8080 |
| **Auth Service** | http://localhost:8088/api/v1 |
| **User Service** | http://localhost:8083/api/v1 |
| **Product Service** | http://localhost:8081/api/v1 |
| **Config Server** | http://localhost:8888 |
| **Consul UI** | http://localhost:8500 |
| **Vault UI** | http://localhost:8200 |
| **MinIO Console** | http://localhost:9001 |

---

## 📖 API Documentation

### Swagger UI (Khuyến nghị)

Truy cập **Swagger UI Aggregation** qua API Gateway:

| Endpoint | Mô tả |
|----------|-------|
| http://localhost:8080/swagger-ui.html | **Swagger UI** - Xem tất cả APIs |
| http://localhost:8080/v3/api-docs | API Gateway OpenAPI JSON |

### Cách sử dụng

1. Truy cập http://localhost:8080/swagger-ui.html
2. Chọn service trong dropdown **"Select a definition"**:
   - **API Gateway** - Metadata của Gateway
   - **Auth Service** - APIs authentication (login, register, introspect)
   - **Product Service** - APIs quản lý sản phẩm
   - **Order Service** - APIs đơn hàng
   - **User Service** - APIs người dùng
3. Nhấn **Explore** để load API documentation

### Truy cập trực tiếp từng Service

| Service | Swagger UI | OpenAPI JSON |
|---------|------------|--------------|
| Auth Service | http://localhost:8088/api/v1/swagger-ui.html | http://localhost:8088/api/v1/v3/api-docs |
| User Service | http://localhost:8083/api/v1/swagger-ui.html | http://localhost:8083/api/v1/v3/api-docs |
| Product Service | http://localhost:8081/api/v1/swagger-ui.html | http://localhost:8081/api/v1/v3/api-docs |

---

## 🔐 Quản lý Secrets với Vault

### Architecture

```
┌───────────────────────────────────────────────────────────────────────────┐
│                            E-commerce System                               │
├───────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │
│  │auth-service │  │user-service │  │product-svc  │  │   api-gateway   │  │
│  │  (8088)     │  │  (8083)     │  │  (8081)     │  │     (8080)      │  │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └────────┬────────┘  │
│         │                │                │                   │           │
│         └────────────────┴────────────────┴───────────────────┘           │
│                                   │                                        │
│                                   ▼                                        │
│                          ┌─────────────────┐                              │
│                          │  HashiCorp Vault │                             │
│                          │   (Port 8200)    │                             │
│                          └─────────────────┘                              │
│                                   │                                        │
│              ┌────────────────────┼────────────────────┐                  │
│              ▼                    ▼                    ▼                  │
│       ┌──────────────┐    ┌──────────────┐    ┌──────────────┐           │
│       │ JWT Secrets  │    │ DB Passwords │    │ API Keys     │           │
│       └──────────────┘    └──────────────┘    └──────────────┘           │
│                                                                            │
└───────────────────────────────────────────────────────────────────────────┘
```

### Secret Paths

| Path | Mô tả | Used By |
|------|-------|---------|
| `secret/ecommerce/auth-service` | JWT keys, token expiration | auth-service |
| `secret/ecommerce/user-service` | Service credentials | user-service |
| `secret/ecommerce/product-service` | API keys | product-service |
| `secret/ecommerce/api-gateway` | Rate limit keys | api-gateway |
| `secret/ecommerce/database/postgres` | PostgreSQL credentials | auth, order, user |
| `secret/ecommerce/database/mongodb` | MongoDB credentials | product-service |
| `secret/ecommerce/database/redis` | Redis credentials | All services |

### Truy cập Vault UI

Mở http://localhost:8200/ui và đăng nhập với token từ file `.env`.

### Managing Secrets

```powershell
# Set environment
$env:VAULT_ADDR = "http://localhost:8200"
$env:VAULT_TOKEN = "your-token-from-env-file"

# List secrets
vault kv list secret/ecommerce/

# Read a secret
vault kv get secret/ecommerce/auth-service

# Update a secret
vault kv patch secret/ecommerce/auth-service jwt-signer-key="new-secret-key"
```

---

## 🌍 Environment Profiles

Hệ thống sử dụng **Spring Profiles** để quản lý cấu hình:

| Profile | Mô tả | Use Case |
|---------|-------|----------|
| `dev` | Development | Local development, debugging |
| `staging` | Staging | Pre-production testing |
| `prod` | Production | Live environment |

### Configuration Comparison

| Setting | Dev | Staging | Prod |
|---------|-----|---------|------|
| **Logging Level** | DEBUG | INFO | WARN |
| **Swagger UI** | ✅ | ✅ | ❌ |
| **Actuator Endpoints** | All | Limited | Minimal |
| **Error Details** | Full | Partial | None |
| **Vault Auth** | Token | AppRole | AppRole+TLS |
| **JPA ddl-auto** | update | validate | none |
| **Rate Limit** | 100/sec | 20/sec | 10/sec |

### Quick Start cho mỗi môi trường

```powershell
# Development
cd infra && .\start-dev.ps1
$env:SPRING_PROFILES_ACTIVE = "dev"

# Staging
cd infra && .\start-staging.ps1
$env:SPRING_PROFILES_ACTIVE = "staging"

# Production
$env:SPRING_PROFILES_ACTIVE = "prod"
# Deploy via Kubernetes or Docker Swarm
```

---

## 🔧 Troubleshooting

### Common Issues

#### 1. Config Server không khởi động được

```
Error: Connection refused to localhost:8888
```

**Solution:**
```powershell
# Kiểm tra Config Server đã chạy chưa
curl http://localhost:8888/actuator/health

# Nếu chưa, start Config Server trước
cd support-services/config-server
mvn spring-boot:run
```

#### 2. Vault connection failed

```
Error: Connection refused to Vault at http://localhost:8200
```

**Solution:**
```powershell
# Kiểm tra Vault container
docker-compose ps vault
docker-compose logs vault

# Restart Vault nếu cần
docker-compose restart vault
```

#### 3. Service không tìm thấy profile config

```
Error: Could not resolve placeholder 'jwt-signer-key'
```

**Solution:**
- Kiểm tra Vault đã được initialize: `.\vault\init-vault.ps1`
- Kiểm tra profile: `echo $env:SPRING_PROFILES_ACTIVE`
- Kiểm tra Config Server có file config cho profile đó

#### 4. Database connection failed

```
Error: Connection to localhost:5432 refused
```

**Solution:**
```powershell
# Kiểm tra database container
docker-compose ps postgres
docker-compose logs postgres

# Kiểm tra credentials
cat .env | Select-String "POSTGRES"
```

### Useful Commands

```powershell
# Xem logs của tất cả services
docker-compose logs -f

# Xem logs của service cụ thể
docker-compose logs -f auth-service

# Restart tất cả services
docker-compose restart

# Xóa tất cả và bắt đầu lại
docker-compose down -v
.\start-dev.ps1

# Kiểm tra config từ Config Server
curl http://localhost:8888/auth-service/dev
```

> 📖 **Tài liệu tham khảo:**
> - [GETTING-STARTED.md](markdown-source/GETTING-STARTED.md) - Hướng dẫn chạy dự án từ A-Z
> - [USE-CASES.md](markdown-source/USE-CASES.md) - Danh sách Use Cases & luồng hoạt động chi tiết

---

## 🤝 Đóng góp

Chúng tôi hoan nghênh mọi đóng góp! Vui lòng xem [CONTRIBUTING.md](CONTRIBUTING.md) để biết thêm chi tiết.

### Development Workflow

1. Fork repository
2. Tạo feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Mở Pull Request

### Code Style

- Sử dụng [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Viết Unit Tests cho tất cả các business logic
- Đảm bảo tất cả tests pass trước khi tạo PR

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

---

## 📞 Liên hệ

- **Email:** datlt244@gmail.com
- **GitHub:** [datlt244-work](https://github.com/datlt244-work)

---

<p align="center">
  Made with ❤️ by Book Shop Team
</p>
