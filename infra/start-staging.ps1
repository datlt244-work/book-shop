<#
.SYNOPSIS
    Start Staging Environment
.DESCRIPTION
    Starts all infrastructure services with staging configuration
#>

Write-Host "`n=== Starting STAGING Environment ===" -ForegroundColor Yellow

# Check if .env exists
$envFile = Join-Path $PSScriptRoot ".env"
if (-not (Test-Path $envFile)) {
    Write-Host "ERROR: .env file not found. Run generate-env.ps1 first." -ForegroundColor Red
    exit 1
}

# Set staging environment
$env:SPRING_PROFILES_ACTIVE = "staging"

# Start infrastructure with staging config
Write-Host "`nStarting infrastructure services (staging mode)..." -ForegroundColor Yellow
docker-compose -f docker-compose.yml -f docker-compose.staging.yml up -d

Write-Host "`n=== Staging Environment Started ===" -ForegroundColor Green
Write-Host "`nNote: In staging, external ports are not exposed." -ForegroundColor Yellow
Write-Host "Services communicate only through the internal network.`n" -ForegroundColor Yellow

