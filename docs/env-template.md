# Environment Variables Template

Copy content below to `infra/.env` file. **DO NOT commit `.env` to version control!**

```bash
# =============================================================================
# ECOMMERCE MICROSERVICES - ENVIRONMENT VARIABLES
# =============================================================================

# =============================================================================
# DATABASE (PostgreSQL)
# =============================================================================
DB_HOST=localhost
DB_PORT=5432
DB_USER=ecom_admin
DB_PASSWORD=your-secure-password-here

# =============================================================================
# REDIS
# =============================================================================
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password-here

# =============================================================================
# JWT CONFIGURATION (CRITICAL - Must be same across ALL services!)
# =============================================================================
# Generate: openssl rand -base64 32
JWT_SIGNER_KEY=your-super-secret-jwt-signing-key-at-least-256-bits-long
JWT_EXPIRATION=3600
JWT_REFRESH_EXPIRATION=86400

# =============================================================================
# CONSUL
# =============================================================================
CONSUL_HOST=localhost
CONSUL_PORT=8500

# =============================================================================
# CONFIG SERVER
# =============================================================================
CONFIG_SERVER_HOST=localhost
CONFIG_SERVER_PORT=8888
CONFIG_SERVER_USER=config
CONFIG_SERVER_PASSWORD=config123

# =============================================================================
# MINIO (Object Storage)
# =============================================================================
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin

# =============================================================================
# EMAIL (SMTP)
# =============================================================================
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# =============================================================================
# SPRING PROFILES
# =============================================================================
SPRING_PROFILES_ACTIVE=dev
```

## Quick Start

1. Create `.env` file in `infra/` folder:
```powershell
# Windows
Copy-Item docs/env-template.md infra/.env
# Then edit infra/.env with your values
```

2. Required values for local development:
   - `DB_PASSWORD` - PostgreSQL password (check docker-compose.yml)
   - `REDIS_PASSWORD` - Redis password (check docker-compose.yml)
   - `JWT_SIGNER_KEY` - Must be at least 32 characters

3. Both auth-service and user-service will automatically load from `infra/.env`

