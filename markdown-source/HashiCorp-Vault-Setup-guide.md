# 🔐 HashiCorp Vault Setup Guide

## Overview

This guide explains how to set up and use HashiCorp Vault for secret management in the E-commerce System.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        E-commerce System                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────────┐  │
│  │ auth-service│    │product-svc  │    │   api-gateway       │  │
│  │             │    │             │    │                     │  │
│  └──────┬──────┘    └──────┬──────┘    └──────────┬──────────┘  │
│         │                  │                       │             │
│         └──────────────────┼───────────────────────┘             │
│                            │                                     │
│                            ▼                                     │
│                   ┌─────────────────┐                           │
│                   │  HashiCorp Vault │                          │
│                   │   (Port 8200)    │                          │
│                   └─────────────────┘                           │
│                            │                                     │
│         ┌──────────────────┼──────────────────┐                 │
│         ▼                  ▼                  ▼                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ JWT Secrets  │  │ DB Passwords │  │ API Keys     │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

## Quick Start

### 1. Generate Environment File (First time only)

```powershell
cd infra
.\generate-env.ps1
```

This creates a `.env` file with **random passwords** for all services.
⚠️ The `.env` file is in `.gitignore` and will NOT be committed!

### 2. Start Infrastructure

```powershell
docker-compose up -d
```

### 3. Initialize Vault

```powershell
.\vault\init-vault.ps1
```

### 4. Access Vault UI

Open [http://localhost:8200/ui](http://localhost:8200/ui) and login with the token from your `.env` file.

## Secret Paths

| Path | Description | Used By |
|------|-------------|---------|
| `secret/ecommerce/auth-service` | JWT keys, token expiration | auth-service |
| `secret/ecommerce/product-service` | API keys | product-service |
| `secret/ecommerce/api-gateway` | Rate limit keys | api-gateway |
| `secret/ecommerce/config-server` | Encryption keys | config-server |
| `secret/ecommerce/database/postgres` | PostgreSQL credentials | auth-service, order-service |
| `secret/ecommerce/database/mongodb` | MongoDB credentials | product-service |
| `secret/ecommerce/database/redis` | Redis credentials | All services |
| `secret/ecommerce/common` | Shared secrets (Kafka, MinIO) | All services |

## Configuration

### 🎯 No .env file needed!

Tất cả secrets được quản lý bởi Vault. Các file `application.yaml` đã có default values cho development.

**Development:** Chỉ cần chạy `docker-compose up -d` và init script.

**Production:** Sử dụng Kubernetes Secrets hoặc CI/CD để inject `VAULT_ROLE_ID` và `VAULT_SECRET_ID`.

### Environment Variables (Optional - chỉ override khi cần)

| Variable | Default | Khi nào cần set? |
|----------|---------|------------------|
| `VAULT_URI` | `http://localhost:8200` | Khi Vault không chạy ở localhost |
| `VAULT_TOKEN` | `ecom-root-token` | Chỉ dùng cho dev |
| `VAULT_ROLE_ID` | - | **Production only** - từ K8s Secrets |
| `VAULT_SECRET_ID` | - | **Production only** - từ K8s Secrets |

### Service Configuration (application.yaml)

```yaml
spring:
  cloud:
    vault:
      uri: ${VAULT_URI:http://localhost:8200}
      token: ${VAULT_TOKEN:ecom-root-token}
      kv:
        enabled: true
        backend: secret
        default-context: ecommerce/auth-service
```

## Authentication Methods

### 1. Token Authentication (Development)

Used in development mode with the root token:

```yaml
spring:
  cloud:
    vault:
      token: ${VAULT_TOKEN:ecom-root-token}
```

### 2. AppRole Authentication (Production)

Recommended for production:

```yaml
spring:
  cloud:
    vault:
      authentication: APPROLE
      app-role:
        role-id: ${VAULT_ROLE_ID}
        secret-id: ${VAULT_SECRET_ID}
        role: ecommerce-services
```

## Managing Secrets

### View Secrets

```bash
# Set environment
export VAULT_ADDR="http://localhost:8200"
export VAULT_TOKEN="ecom-root-token"

# List secrets
vault kv list secret/ecommerce/

# Read a secret
vault kv get secret/ecommerce/auth-service
```

### Update Secrets

```bash
# Update JWT signer key
vault kv patch secret/ecommerce/auth-service jwt-signer-key="new-secret-key"

# Update database password
vault kv patch secret/ecommerce/database/postgres password="new-password"
```

### Rotate Secrets

```bash
# Generate new secret and update
NEW_KEY=$(openssl rand -base64 64)
vault kv patch secret/ecommerce/auth-service jwt-signer-key="$NEW_KEY"
```

## Security Best Practices

### Development

- ✅ Use dev mode with root token for local development
- ✅ Initialize Vault with random passwords using init scripts
- ⚠️ Never commit real secrets to version control

### Production

- 🔒 Use AppRole authentication instead of root token
- 🔒 Enable Vault audit logging
- 🔒 Use auto-unsealing with cloud KMS
- 🔒 Implement secret rotation policies
- 🔒 Use separate Vault namespaces per environment
- 🔒 Enable TLS for Vault communication

### Production Configuration Example

```yaml
spring:
  cloud:
    vault:
      uri: https://vault.production.internal:8200
      authentication: APPROLE
      app-role:
        role-id: ${VAULT_ROLE_ID}
        secret-id: ${VAULT_SECRET_ID}
      ssl:
        trust-store: classpath:vault-truststore.jks
        trust-store-password: ${VAULT_TRUSTSTORE_PASSWORD}
```

## Troubleshooting

### Common Issues

**1. Connection Refused**
```
Error: Connection refused to Vault at http://localhost:8200
```
Solution: Ensure Vault container is running:
```bash
docker-compose ps vault
docker-compose logs vault
```

**2. Permission Denied**
```
Error: permission denied
```
Solution: Check if the token has the correct policy:
```bash
vault token lookup
vault policy read ecommerce-policy
```

**3. Secret Not Found**
```
Error: secret not found at path secret/ecommerce/auth-service
```
Solution: Verify the secret exists:
```bash
vault kv get secret/ecommerce/auth-service
```

### Health Check

```bash
# Check Vault status
vault status

# Check seal status
curl http://localhost:8200/v1/sys/seal-status
```

## References

- [Spring Cloud Vault Documentation](https://spring.io/projects/spring-cloud-vault)
- [HashiCorp Vault Documentation](https://developer.hashicorp.com/vault/docs)
- [Vault AppRole Auth Method](https://developer.hashicorp.com/vault/docs/auth/approle)

