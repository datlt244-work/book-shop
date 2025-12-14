# Policy for E-commerce services to read secrets
# Each service can only read its own secrets

# Auth Service secrets
path "secret/data/ecommerce/auth-service" {
  capabilities = ["read", "list"]
}

# Product Service secrets
path "secret/data/ecommerce/product-service" {
  capabilities = ["read", "list"]
}

# Order Service secrets
path "secret/data/ecommerce/order-service" {
  capabilities = ["read", "list"]
}

# User Service secrets
path "secret/data/ecommerce/user-service" {
  capabilities = ["read", "list"]
}

# Config Server secrets
path "secret/data/ecommerce/config-server" {
  capabilities = ["read", "list"]
}

# API Gateway secrets
path "secret/data/ecommerce/api-gateway" {
  capabilities = ["read", "list"]
}

# Common/shared secrets accessible by all services
path "secret/data/ecommerce/common" {
  capabilities = ["read", "list"]
}

# Database credentials
path "secret/data/ecommerce/database/*" {
  capabilities = ["read", "list"]
}

