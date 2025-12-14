#!/bin/bash
# =============================================================================
# Vault Initialization Script for E-commerce System
# Run this script after Vault container is up and running
# =============================================================================

set -e

VAULT_ADDR="http://localhost:8200"
VAULT_TOKEN="ecom-root-token"

echo "🔐 Initializing Vault for E-commerce System..."

# Wait for Vault to be ready
echo "⏳ Waiting for Vault to be ready..."
until curl -s ${VAULT_ADDR}/v1/sys/health > /dev/null 2>&1; do
    sleep 2
done
echo "✅ Vault is ready!"

# Export Vault address and token
export VAULT_ADDR
export VAULT_TOKEN

# Enable KV secrets engine v2
echo "📦 Enabling KV secrets engine..."
vault secrets enable -path=secret kv-v2 2>/dev/null || echo "KV engine already enabled"

# =============================================================================
# Store Database Credentials
# =============================================================================
echo "🗄️  Storing database credentials..."

# PostgreSQL credentials
vault kv put secret/ecommerce/database/postgres \
    username="admin" \
    password="$(openssl rand -base64 32)" \
    url="jdbc:postgresql://postgres:5432"

# MongoDB credentials
vault kv put secret/ecommerce/database/mongodb \
    username="admin" \
    password="$(openssl rand -base64 32)" \
    uri="mongodb://mongodb:27017"

# Redis credentials
vault kv put secret/ecommerce/database/redis \
    password="$(openssl rand -base64 32)" \
    host="redis" \
    port="6379"

# =============================================================================
# Store Service-specific Secrets
# =============================================================================
echo "🔑 Storing service secrets..."

# Auth Service secrets
vault kv put secret/ecommerce/auth-service \
    jwt-signer-key="$(openssl rand -base64 64)" \
    jwt-expiration="3600" \
    refresh-token-expiration="86400"

# Config Server secrets
vault kv put secret/ecommerce/config-server \
    encrypt-key="$(openssl rand -base64 32)" \
    username="config" \
    password="$(openssl rand -base64 16)"

# API Gateway secrets
vault kv put secret/ecommerce/api-gateway \
    rate-limit-key="$(openssl rand -base64 16)"

# Product Service secrets
vault kv put secret/ecommerce/product-service \
    api-key="$(openssl rand -base64 32)"

# =============================================================================
# Store Common/Shared Secrets
# =============================================================================
echo "🌐 Storing common secrets..."

vault kv put secret/ecommerce/common \
    kafka-username="kafka-user" \
    kafka-password="$(openssl rand -base64 16)" \
    minio-access-key="$(openssl rand -base64 16)" \
    minio-secret-key="$(openssl rand -base64 32)"

# =============================================================================
# Create Policy and AppRole for Services
# =============================================================================
echo "📜 Creating policies..."

vault policy write ecommerce-policy /vault/policies/ecommerce-policy.hcl

# Enable AppRole auth method
echo "🔐 Enabling AppRole authentication..."
vault auth enable approle 2>/dev/null || echo "AppRole already enabled"

# Create AppRole for services
vault write auth/approle/role/ecommerce-services \
    token_policies="ecommerce-policy" \
    token_ttl=1h \
    token_max_ttl=4h \
    secret_id_ttl=0 \
    secret_id_num_uses=0

# Get Role ID and Secret ID
ROLE_ID=$(vault read -field=role_id auth/approle/role/ecommerce-services/role-id)
SECRET_ID=$(vault write -field=secret_id -f auth/approle/role/ecommerce-services/secret-id)

echo ""
echo "============================================================================="
echo "✅ Vault initialization complete!"
echo "============================================================================="
echo ""
echo "🔐 AppRole Credentials (save these securely!):"
echo "   VAULT_ROLE_ID: ${ROLE_ID}"
echo "   VAULT_SECRET_ID: ${SECRET_ID}"
echo ""
echo "📋 Vault UI: ${VAULT_ADDR}/ui"
echo "   Token: ${VAULT_TOKEN}"
echo ""
echo "⚠️  IMPORTANT: In production, use proper unsealing and don't use dev mode!"
echo "============================================================================="

