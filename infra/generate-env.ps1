<#
.SYNOPSIS
    Generate .env file with random passwords for Docker Compose
.DESCRIPTION
    This script generates a .env file with secure random passwords.
    Run this ONCE when setting up the project for the first time.
    The .env file is NOT committed to git.
#>

$envFile = Join-Path $PSScriptRoot ".env"

# Check if .env already exists
if (Test-Path $envFile) {
    $response = Read-Host ".env file already exists. Overwrite? (y/N)"
    if ($response -ne "y" -and $response -ne "Y") {
        Write-Host "Aborted. Keeping existing .env file." -ForegroundColor Yellow
        exit 0
    }
}

# Function to generate random password
function Get-RandomPassword {
    param([int]$Length = 24)
    $bytes = New-Object byte[] $Length
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($bytes)
    # Use URL-safe base64 and trim to desired length
    $password = [Convert]::ToBase64String($bytes) -replace '[+/=]', ''
    return $password.Substring(0, [Math]::Min($Length, $password.Length))
}

Write-Host "`n=== Generating .env file with random passwords ===" -ForegroundColor Cyan

# Generate random passwords
$postgresPass = Get-RandomPassword
$mongoPass = Get-RandomPassword
$redisPass = Get-RandomPassword
$minioPass = Get-RandomPassword
$vaultToken = Get-RandomPassword -Length 32

# Create .env content
$envContent = @"
# =============================================================================
# E-commerce System - Docker Compose Environment Variables
# Generated on: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
# =============================================================================
# WARNING: This file contains secrets. DO NOT commit to git!
# =============================================================================

# PostgreSQL
POSTGRES_USER=ecom_admin
POSTGRES_PASSWORD=$postgresPass
POSTGRES_DB=ecom_db

# MongoDB
MONGO_USER=ecom_admin
MONGO_PASSWORD=$mongoPass

# Redis
REDIS_PASSWORD=$redisPass

# MinIO
MINIO_USER=ecom_admin
MINIO_PASSWORD=$minioPass

# Vault
VAULT_TOKEN=$vaultToken
"@

# Write to file
$envContent | Out-File -FilePath $envFile -Encoding UTF8 -NoNewline

Write-Host "`n.env file created successfully!" -ForegroundColor Green
Write-Host "Location: $envFile" -ForegroundColor Gray

Write-Host "`n=== Generated Credentials ===" -ForegroundColor Cyan
Write-Host "PostgreSQL: ecom_admin / $postgresPass" -ForegroundColor White
Write-Host "MongoDB:    ecom_admin / $mongoPass" -ForegroundColor White
Write-Host "Redis:      $redisPass" -ForegroundColor White
Write-Host "MinIO:      ecom_admin / $minioPass" -ForegroundColor White
Write-Host "Vault:      $vaultToken" -ForegroundColor White

Write-Host "`n=== Next Steps ===" -ForegroundColor Yellow
Write-Host "1. Run: docker-compose up -d" -ForegroundColor White
Write-Host "2. Run: .\vault\init-vault.ps1" -ForegroundColor White
Write-Host "`nIMPORTANT: Save these credentials securely!" -ForegroundColor Red
Write-Host "The .env file is in .gitignore and will NOT be committed.`n" -ForegroundColor Yellow

