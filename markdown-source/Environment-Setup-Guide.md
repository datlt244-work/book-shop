# 🚀 Environment Setup Guide

## Mục lục

- [Tổng quan](#tổng-quan)
- [Development Environment](#development-environment)
- [Staging Environment](#staging-environment)
- [Production Environment](#production-environment)
- [So sánh các môi trường](#so-sánh-các-môi-trường)
- [Troubleshooting](#troubleshooting)

---

## Tổng quan

Hệ thống E-commerce sử dụng **Spring Profiles** để quản lý cấu hình cho các môi trường khác nhau:

| Profile | Mô tả | Use Case |
|---------|-------|----------|
| `dev` | Development | Local development, debugging |
| `staging` | Staging | Pre-production testing |
| `prod` | Production | Live environment |

### Kiến trúc cấu hình

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Config Loading Flow                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│   ┌──────────────┐      ┌──────────────┐      ┌──────────────┐      │
│   │   Service    │ ──▶  │ Config Server│ ──▶  │    Vault     │      │
│   │ application  │      │  (Port 8888) │      │ (Port 8200)  │      │
│   │    .yaml     │      │              │      │              │      │
│   └──────────────┘      └──────────────┘      └──────────────┘      │
│         │                      │                     │               │
│         ▼                      ▼                     ▼               │
│   SPRING_PROFILES       Profile-specific        Secrets             │
│   _ACTIVE=dev           configurations         (JWT, DB pass)       │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Development Environment

### Prerequisites

- Docker Desktop đã cài đặt và đang chạy
- Java 21+
- Maven 3.9+
- PowerShell 7+ (Windows)

### Quick Start

```powershell
# 1. Clone repository
git clone https://github.com/your-org/ecommerce-system.git
cd ecommerce-system/infra

# 2. Chạy script khởi tạo (tự động tạo .env và start Docker)
.\start-dev.ps1
```

### Manual Setup (Step by Step)

#### Bước 1: Tạo file .env

```powershell
cd infra
.\generate-env.ps1
```

Output mẫu:
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

#### Bước 2: Khởi động Infrastructure

```powershell
docker-compose up -d
```

Kiểm tra trạng thái:
```powershell
docker-compose ps
```

#### Bước 3: Khởi tạo Vault

```powershell
.\vault\init-vault.ps1
```

#### Bước 4: Chạy Config Server

```powershell
cd ../support-services/config-server
$env:SPRING_PROFILES_ACTIVE = "native"
mvn spring-boot:run
```

#### Bước 5: Chạy các Services

**Auth Service:**
```powershell
cd auth-service
$env:SPRING_PROFILES_ACTIVE = "dev"
mvn spring-boot:run
```

**API Gateway:**
```powershell
cd api-gateway
$env:SPRING_PROFILES_ACTIVE = "dev"
mvn spring-boot:run
```

**Product Service:**
```powershell
cd core-services/product-service
$env:SPRING_PROFILES_ACTIVE = "dev"
mvn spring-boot:run
```

### Development URLs

| Service | URL |
|---------|-----|
| API Gateway | http://localhost:8080 |
| Auth Service | http://localhost:8088/api/v1 |
| Auth Swagger | http://localhost:8088/api/v1/swagger-ui.html |
| Product Service | http://localhost:8081/api/v1 |
| Config Server | http://localhost:8888 |
| Consul UI | http://localhost:8500 |
| Vault UI | http://localhost:8200 |
| MinIO Console | http://localhost:9001 |

### Development Features

- ✅ Debug logging enabled
- ✅ Swagger UI enabled
- ✅ All actuator endpoints exposed
- ✅ Full error stacktraces
- ✅ JPA ddl-auto: update (auto schema update)
- ✅ External ports exposed for debugging
- ✅ Relaxed rate limiting (100 req/sec)

---

## Staging Environment

### Prerequisites

- Tất cả prerequisites của Development
- Đã có `.env` file với credentials

### Quick Start

```powershell
cd infra
.\start-staging.ps1
```

### Manual Setup

#### Bước 1: Start Infrastructure với Staging Config

```powershell
cd infra
docker-compose -f docker-compose.yml -f docker-compose.staging.yml up -d
```

#### Bước 2: Chạy Services với Staging Profile

```powershell
# Auth Service
$env:SPRING_PROFILES_ACTIVE = "staging"
$env:VAULT_ROLE_ID = "your-role-id"
$env:VAULT_SECRET_ID = "your-secret-id"
mvn spring-boot:run
```

### Staging Differences từ Dev

| Feature | Dev | Staging |
|---------|-----|---------|
| Vault Auth | Token | AppRole |
| External Ports | Exposed | Internal only |
| Logging | DEBUG | INFO |
| Rate Limit | 100/sec | 20/sec |
| Error Details | Full | Partial |
| JPA ddl-auto | update | validate |

### Staging URLs

> **Note:** Trong Staging, các services chỉ accessible qua internal network.
> Sử dụng API Gateway để truy cập.

| Service | Internal URL |
|---------|--------------|
| API Gateway | api-gateway:8080 |
| Auth Service | auth-service:8088 |
| Product Service | product-service:8081 |

---

## Production Environment

### ⚠️ Important Security Notes

1. **KHÔNG sử dụng docker-compose cho production thực tế**
2. Sử dụng **Kubernetes** hoặc **Docker Swarm**
3. Sử dụng **managed services** (AWS RDS, Azure CosmosDB, etc.)
4. Cấu hình **TLS/SSL certificates**
5. Setup **monitoring và alerting**

### Prerequisites

- Kubernetes cluster hoặc Docker Swarm
- Managed database services (recommended)
- TLS certificates
- Vault server (production mode, not dev mode)
- CI/CD pipeline configured

### Required Environment Variables

```bash
# Database
POSTGRES_HOST=your-postgres-host
POSTGRES_USER=your-postgres-user
POSTGRES_PASSWORD=your-secure-password

MONGO_HOST=your-mongo-host
MONGO_USER=your-mongo-user
MONGO_PASSWORD=your-secure-password

REDIS_HOST=your-redis-host
REDIS_PASSWORD=your-secure-password

# Vault (AppRole authentication)
VAULT_URI=https://vault.your-domain.com:8200
VAULT_ROLE_ID=your-role-id
VAULT_SECRET_ID=your-secret-id

# MinIO / S3
MINIO_USER=your-minio-user
MINIO_PASSWORD=your-secure-password

# Consul
CONSUL_HOST=consul.your-domain.com
CONSUL_ENCRYPT_KEY=your-encrypt-key

# Frontend
FRONTEND_URL=https://your-frontend-domain.com

# SSL (if using embedded SSL)
SSL_ENABLED=true
SSL_KEYSTORE=/path/to/keystore.p12
SSL_KEYSTORE_PASSWORD=your-keystore-password
```

### Docker Compose (Reference Only)

```powershell
cd infra
.\start-prod.ps1
```

### Kubernetes Deployment (Recommended)

```yaml
# Example Kubernetes deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: auth-service
        image: your-registry/auth-service:latest
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: VAULT_ROLE_ID
          valueFrom:
            secretKeyRef:
              name: vault-credentials
              key: role-id
        - name: VAULT_SECRET_ID
          valueFrom:
            secretKeyRef:
              name: vault-credentials
              key: secret-id
```

### Production Security Checklist

- [ ] Vault running in production mode (not dev mode)
- [ ] Vault auto-unseal configured (AWS KMS, Azure Key Vault, etc.)
- [ ] TLS enabled for all services
- [ ] Database connections use SSL
- [ ] Secrets stored in Kubernetes Secrets or Vault
- [ ] Network policies configured
- [ ] Rate limiting enabled
- [ ] Swagger UI disabled
- [ ] Debug logging disabled
- [ ] Error details hidden from responses
- [ ] CORS configured for specific origins only
- [ ] Health checks configured
- [ ] Monitoring and alerting set up

### Production Features

- ❌ Debug logging disabled
- ❌ Swagger UI disabled
- ⚠️ Limited actuator endpoints (health, info, prometheus)
- ❌ Error stacktraces hidden
- ✅ JPA ddl-auto: none (manual migrations)
- ❌ External ports not exposed
- ✅ Strict rate limiting (10 req/sec)
- ✅ AppRole authentication for Vault
- ✅ TLS/SSL enabled

---

## So sánh các môi trường

### Configuration Comparison

| Setting | Dev | Staging | Prod |
|---------|-----|---------|------|
| **spring.profiles.active** | dev | staging | prod |
| **Logging Level** | DEBUG | INFO | WARN |
| **Swagger UI** | ✅ | ✅ | ❌ |
| **Actuator** | All | Limited | Minimal |
| **Error Details** | Full | Partial | None |
| **Vault Auth** | Token | AppRole | AppRole+TLS |
| **JPA ddl-auto** | update | validate | none |

### Rate Limiting Comparison

| Endpoint Type | Dev | Staging | Prod |
|---------------|-----|---------|------|
| Default APIs | 100/sec | 20/sec | 10/sec |
| Auth APIs | 50/sec | 10/sec | 5/sec |
| Burst Capacity | 200 | 40 | 20 |

### Database Connection Pool

| Setting | Dev | Staging | Prod |
|---------|-----|---------|------|
| Max Pool Size | 5 | 10 | 20 |
| Min Idle | 2 | 5 | 10 |
| Connection Timeout | 20s | 30s | 30s |

---

## Troubleshooting

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
- Kiểm tra Vault đã được initialize chưa: `.\vault\init-vault.ps1`
- Kiểm tra profile đúng chưa: `echo $env:SPRING_PROFILES_ACTIVE`
- Kiểm tra Config Server có file config cho profile đó không

#### 4. Rate limit exceeded

```
HTTP 429 Too Many Requests
```

**Solution:**
- Đợi 1 giây và thử lại
- Trong dev, tăng rate limit trong `api-gateway-dev.yaml`
- Kiểm tra Redis đang chạy: `docker-compose ps redis`

#### 5. Database connection failed

```
Error: Connection to localhost:5432 refused
```

**Solution:**
```powershell
# Kiểm tra database container
docker-compose ps postgres

# Kiểm tra logs
docker-compose logs postgres

# Kiểm tra credentials trong .env
cat .env | grep POSTGRES
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

# Kiểm tra secrets trong Vault
$env:VAULT_TOKEN = "your-token"
curl -H "X-Vault-Token: $env:VAULT_TOKEN" http://localhost:8200/v1/secret/data/ecommerce/auth-service
```

---

## Quick Reference Card

### Development
```powershell
cd infra && .\start-dev.ps1
$env:SPRING_PROFILES_ACTIVE = "dev"
mvn spring-boot:run
```

### Staging
```powershell
cd infra && .\start-staging.ps1
$env:SPRING_PROFILES_ACTIVE = "staging"
$env:VAULT_ROLE_ID = "xxx"
$env:VAULT_SECRET_ID = "xxx"
mvn spring-boot:run
```

### Production
```powershell
# Set all required env vars first!
$env:SPRING_PROFILES_ACTIVE = "prod"
# Deploy via Kubernetes or Docker Swarm
```

