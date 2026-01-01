<#
.SYNOPSIS
    Vault Initialization Script for E-commerce System (Windows PowerShell)
.DESCRIPTION
    Run this script after Vault container is up and running
#>

# Configuration
$VaultAddr = "http://localhost:8200"

# Try to read credentials from .env file
$envFile = Join-Path $PSScriptRoot "..\.env"
$VaultToken = "ecom-root-token"  # Default fallback

# Initialize credential variables
$EnvPostgresUser = "admin"
$EnvPostgresPass = ""
$EnvMongoUser = "admin"
$EnvMongoPass = ""
$EnvRedisPass = ""
$EnvMinioUser = ""
$EnvMinioPass = ""

if (Test-Path $envFile) {
    $envContent = Get-Content $envFile
    foreach ($line in $envContent) {
        if ($line -match "^VAULT_TOKEN=(.+)$") {
            $VaultToken = $Matches[1].Trim()
            Write-Host "Using VAULT_TOKEN from .env file" -ForegroundColor Gray
        }
        if ($line -match "^POSTGRES_USER=(.+)$") {
            $EnvPostgresUser = $Matches[1].Trim()
        }
        if ($line -match "^POSTGRES_PASSWORD=(.+)$") {
            $EnvPostgresPass = $Matches[1].Trim()
        }
        if ($line -match "^MONGO_USER=(.+)$") {
            $EnvMongoUser = $Matches[1].Trim()
        }
        if ($line -match "^MONGO_PASSWORD=(.+)$") {
            $EnvMongoPass = $Matches[1].Trim()
        }
        if ($line -match "^REDIS_PASSWORD=(.+)$") {
            $EnvRedisPass = $Matches[1].Trim()
        }
        if ($line -match "^MINIO_USER=(.+)$") {
            $EnvMinioUser = $Matches[1].Trim()
        }
        if ($line -match "^MINIO_PASSWORD=(.+)$") {
            $EnvMinioPass = $Matches[1].Trim()
        }
    }
    Write-Host "Loaded credentials from .env file" -ForegroundColor Gray
}

Write-Host "`n=== Initializing Vault for E-commerce System ===" -ForegroundColor Cyan

# Wait for Vault to be ready
Write-Host "Waiting for Vault to be ready..." -ForegroundColor Yellow
$maxRetries = 30
$retryCount = 0

do {
    Start-Sleep -Seconds 2
    $retryCount++
    try {
        $health = Invoke-RestMethod -Uri "$VaultAddr/v1/sys/health" -Method Get -ErrorAction Stop
        $vaultReady = $true
        Write-Host "Vault is ready!" -ForegroundColor Green
    } catch {
        $vaultReady = $false
        Write-Host "Waiting... ($retryCount/$maxRetries)" -ForegroundColor Gray
    }
} while ((-not $vaultReady) -and ($retryCount -lt $maxRetries))

if (-not $vaultReady) {
    Write-Host "ERROR: Vault is not responding. Make sure docker-compose is running." -ForegroundColor Red
    exit 1
}

# Function to generate random password
function Get-RandomPassword {
    param(
        [int]$Length = 32
    )
    $bytes = New-Object byte[] $Length
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($bytes)
    return [Convert]::ToBase64String($bytes)
}

# Headers for Vault API
$headers = @{
    "X-Vault-Token" = $VaultToken
    "Content-Type" = "application/json"
}

# =============================================================================
# Enable KV secrets engine v2
# =============================================================================
Write-Host "`nEnabling KV secrets engine..." -ForegroundColor Yellow
try {
    $kvBody = @{ type = "kv-v2" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$VaultAddr/v1/sys/mounts/secret" -Method Post -Headers $headers -Body $kvBody -ErrorAction Stop
    Write-Host "  KV engine enabled" -ForegroundColor Green
} catch {
    if ($_.Exception.Response.StatusCode -eq 400) {
        Write-Host "  KV engine already enabled" -ForegroundColor Gray
    } else {
        Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# =============================================================================
# Store Database Credentials (Read from .env, fallback to random)
# =============================================================================
Write-Host "`nStoring database credentials..." -ForegroundColor Yellow

# PostgreSQL credentials - Use .env values or generate new
$postgresPass = if ($EnvPostgresPass) { $EnvPostgresPass } else { Get-RandomPassword }
$postgresUser = if ($EnvPostgresUser) { $EnvPostgresUser } else { "admin" }
$postgresData = @{
    data = @{
        username = $postgresUser
        password = $postgresPass
        url = "jdbc:postgresql://postgres:5432"
    }
} | ConvertTo-Json -Depth 3

try {
    Invoke-RestMethod -Uri "$VaultAddr/v1/secret/data/ecommerce/database/postgres" -Method Post -Headers $headers -Body $postgresData -ErrorAction Stop
    Write-Host "  PostgreSQL credentials stored (user: $postgresUser)" -ForegroundColor Green
} catch {
    Write-Host "  Error storing PostgreSQL: $($_.Exception.Message)" -ForegroundColor Red
}

# MongoDB credentials - Use .env values or generate new
$mongoPass = if ($EnvMongoPass) { $EnvMongoPass } else { Get-RandomPassword }
$mongoUser = if ($EnvMongoUser) { $EnvMongoUser } else { "admin" }
$mongoData = @{
    data = @{
        username = $mongoUser
        password = $mongoPass
        uri = "mongodb://mongodb:27017"
    }
} | ConvertTo-Json -Depth 3

try {
    Invoke-RestMethod -Uri "$VaultAddr/v1/secret/data/ecommerce/database/mongodb" -Method Post -Headers $headers -Body $mongoData -ErrorAction Stop
    Write-Host "  MongoDB credentials stored (user: $mongoUser)" -ForegroundColor Green
} catch {
    Write-Host "  Error storing MongoDB: $($_.Exception.Message)" -ForegroundColor Red
}

# Redis credentials - Use .env values or generate new
$redisPass = if ($EnvRedisPass) { $EnvRedisPass } else { Get-RandomPassword }
$redisData = @{
    data = @{
        password = $redisPass
        host = "redis"
        port = "6379"
    }
} | ConvertTo-Json -Depth 3

try {
    Invoke-RestMethod -Uri "$VaultAddr/v1/secret/data/ecommerce/database/redis" -Method Post -Headers $headers -Body $redisData -ErrorAction Stop
    Write-Host "  Redis credentials stored" -ForegroundColor Green
} catch {
    Write-Host "  Error storing Redis: $($_.Exception.Message)" -ForegroundColor Red
}

# =============================================================================
# Store Service-specific Secrets
# =============================================================================
Write-Host "`nStoring service secrets..." -ForegroundColor Yellow

# Auth Service secrets
$jwtKey = Get-RandomPassword -Length 64
$authData = @{
    data = @{
        "jwt-signer-key" = $jwtKey
        "jwt-expiration" = "3600"
        "refresh-token-expiration" = "86400"
    }
} | ConvertTo-Json -Depth 3

try {
    Invoke-RestMethod -Uri "$VaultAddr/v1/secret/data/ecommerce/auth-service" -Method Post -Headers $headers -Body $authData -ErrorAction Stop
    Write-Host "  Auth service secrets stored" -ForegroundColor Green
} catch {
    Write-Host "  Error storing auth-service: $($_.Exception.Message)" -ForegroundColor Red
}

# Config Server secrets
$encryptKey = Get-RandomPassword
$configPass = Get-RandomPassword -Length 16
$configData = @{
    data = @{
        "encrypt-key" = $encryptKey
        "username" = "config"
        "password" = $configPass
    }
} | ConvertTo-Json -Depth 3

try {
    Invoke-RestMethod -Uri "$VaultAddr/v1/secret/data/ecommerce/config-server" -Method Post -Headers $headers -Body $configData -ErrorAction Stop
    Write-Host "  Config server secrets stored" -ForegroundColor Green
} catch {
    Write-Host "  Error storing config-server: $($_.Exception.Message)" -ForegroundColor Red
}

# API Gateway secrets
$rateLimitKey = Get-RandomPassword -Length 16
$gatewayData = @{
    data = @{
        "rate-limit-key" = $rateLimitKey
    }
} | ConvertTo-Json -Depth 3

try {
    Invoke-RestMethod -Uri "$VaultAddr/v1/secret/data/ecommerce/api-gateway" -Method Post -Headers $headers -Body $gatewayData -ErrorAction Stop
    Write-Host "  API Gateway secrets stored" -ForegroundColor Green
} catch {
    Write-Host "  Error storing api-gateway: $($_.Exception.Message)" -ForegroundColor Red
}

# Product Service secrets
$productApiKey = Get-RandomPassword
$productData = @{
    data = @{
        "api-key" = $productApiKey
    }
} | ConvertTo-Json -Depth 3

try {
    Invoke-RestMethod -Uri "$VaultAddr/v1/secret/data/ecommerce/product-service" -Method Post -Headers $headers -Body $productData -ErrorAction Stop
    Write-Host "  Product service secrets stored" -ForegroundColor Green
} catch {
    Write-Host "  Error storing product-service: $($_.Exception.Message)" -ForegroundColor Red
}

# =============================================================================
# Store Common/Shared Secrets
# =============================================================================
Write-Host "`nStoring common secrets..." -ForegroundColor Yellow

# Use .env values for MinIO or generate new
$kafkaPass = Get-RandomPassword -Length 16
$minioAccess = if ($EnvMinioUser) { $EnvMinioUser } else { Get-RandomPassword -Length 16 }
$minioSecret = if ($EnvMinioPass) { $EnvMinioPass } else { Get-RandomPassword }
$commonData = @{
    data = @{
        "kafka-username" = "kafka-user"
        "kafka-password" = $kafkaPass
        "minio-access-key" = $minioAccess
        "minio-secret-key" = $minioSecret
    }
} | ConvertTo-Json -Depth 3

try {
    Invoke-RestMethod -Uri "$VaultAddr/v1/secret/data/ecommerce/common" -Method Post -Headers $headers -Body $commonData -ErrorAction Stop
    Write-Host "  Common secrets stored" -ForegroundColor Green
} catch {
    Write-Host "  Error storing common: $($_.Exception.Message)" -ForegroundColor Red
}

# =============================================================================
# Create Policy
# =============================================================================
Write-Host "`nCreating policies..." -ForegroundColor Yellow

$policyPath = Join-Path $PSScriptRoot "policies\ecommerce-policy.hcl"
if (Test-Path $policyPath) {
    $policyContent = Get-Content -Path $policyPath -Raw
    $policyBody = @{ policy = $policyContent } | ConvertTo-Json
    
    try {
        Invoke-RestMethod -Uri "$VaultAddr/v1/sys/policies/acl/ecommerce-policy" -Method Put -Headers $headers -Body $policyBody -ErrorAction Stop
        Write-Host "  Policy created" -ForegroundColor Green
    } catch {
        Write-Host "  Error creating policy: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "  Policy file not found: $policyPath" -ForegroundColor Red
}

# =============================================================================
# Enable AppRole auth method
# =============================================================================
Write-Host "`nEnabling AppRole authentication..." -ForegroundColor Yellow

try {
    $approleBody = @{ type = "approle" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$VaultAddr/v1/sys/auth/approle" -Method Post -Headers $headers -Body $approleBody -ErrorAction Stop
    Write-Host "  AppRole enabled" -ForegroundColor Green
} catch {
    if ($_.Exception.Response.StatusCode -eq 400) {
        Write-Host "  AppRole already enabled" -ForegroundColor Gray
    } else {
        Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Create AppRole for services
$roleBody = @{
    token_policies = @("ecommerce-policy")
    token_ttl = "1h"
    token_max_ttl = "4h"
    secret_id_ttl = "0"
    secret_id_num_uses = 0
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri "$VaultAddr/v1/auth/approle/role/ecommerce-services" -Method Post -Headers $headers -Body $roleBody -ErrorAction Stop
    Write-Host "  AppRole 'ecommerce-services' created" -ForegroundColor Green
} catch {
    Write-Host "  Error creating role: $($_.Exception.Message)" -ForegroundColor Red
}

# Get Role ID and Secret ID
$roleId = ""
$secretId = ""

try {
    $roleIdResponse = Invoke-RestMethod -Uri "$VaultAddr/v1/auth/approle/role/ecommerce-services/role-id" -Method Get -Headers $headers -ErrorAction Stop
    $roleId = $roleIdResponse.data.role_id
} catch {
    Write-Host "  Could not get Role ID" -ForegroundColor Red
}

try {
    $secretIdResponse = Invoke-RestMethod -Uri "$VaultAddr/v1/auth/approle/role/ecommerce-services/secret-id" -Method Post -Headers $headers -ErrorAction Stop
    $secretId = $secretIdResponse.data.secret_id
} catch {
    Write-Host "  Could not get Secret ID" -ForegroundColor Red
}

# =============================================================================
# Summary
# =============================================================================
Write-Host "`n=============================================================================" -ForegroundColor Green
Write-Host "Vault initialization complete!" -ForegroundColor Green
Write-Host "=============================================================================" -ForegroundColor Green

Write-Host "`nAppRole Credentials (for production):" -ForegroundColor Cyan
Write-Host "  VAULT_ROLE_ID: $roleId" -ForegroundColor White
Write-Host "  VAULT_SECRET_ID: $secretId" -ForegroundColor White

Write-Host "`nVault UI: $VaultAddr/ui" -ForegroundColor Cyan
Write-Host "  Token: $VaultToken" -ForegroundColor White

Write-Host "`n=============================================================================" -ForegroundColor Yellow
Write-Host "MANUAL SETUP REQUIRED - Email (SMTP) Secrets" -ForegroundColor Yellow
Write-Host "=============================================================================" -ForegroundColor Yellow
Write-Host "Run this command to add email secrets (replace with your values):" -ForegroundColor White
Write-Host @"

# Using Vault CLI:
vault kv put secret/ecommerce/mail `
    host=smtp.gmail.com `
    port=587 `
    username=your-email@gmail.com `
    password=your-app-password

# Or using Vault UI:
# 1. Go to $VaultAddr/ui
# 2. Navigate to: secret > ecommerce > mail
# 3. Add keys: host, port, username, password

"@ -ForegroundColor Gray

Write-Host "IMPORTANT: In production, use proper unsealing and do not use dev mode!" -ForegroundColor Yellow
Write-Host "=============================================================================`n" -ForegroundColor Green

