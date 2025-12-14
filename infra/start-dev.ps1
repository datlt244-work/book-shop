<#
.SYNOPSIS
    Start Development Environment
.DESCRIPTION
    Starts all infrastructure services and sets up the development environment
#>

Write-Host "`n=== Starting DEVELOPMENT Environment ===" -ForegroundColor Cyan

# Check if .env exists
$envFile = Join-Path $PSScriptRoot ".env"
if (-not (Test-Path $envFile)) {
    Write-Host "Generating .env file..." -ForegroundColor Yellow
    & "$PSScriptRoot\generate-env.ps1"
}

# Start infrastructure
Write-Host "`nStarting infrastructure services..." -ForegroundColor Yellow
docker-compose up -d

# Wait for services to be healthy
Write-Host "`nWaiting for services to be healthy..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# Check Vault
$vaultReady = $false
$retries = 0
while (-not $vaultReady -and $retries -lt 30) {
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:8200/v1/sys/health" -ErrorAction Stop
        $vaultReady = $true
    } catch {
        $retries++
        Start-Sleep -Seconds 2
    }
}

if ($vaultReady) {
    Write-Host "Vault is ready!" -ForegroundColor Green
    
    # Initialize Vault if needed
    Write-Host "`nInitializing Vault..." -ForegroundColor Yellow
    & "$PSScriptRoot\vault\init-vault.ps1"
} else {
    Write-Host "Warning: Vault is not responding" -ForegroundColor Red
}

Write-Host "`n=== Development Environment Ready ===" -ForegroundColor Green
Write-Host "`nServices:" -ForegroundColor Cyan
Write-Host "  - Consul UI:     http://localhost:8500" -ForegroundColor White
Write-Host "  - Vault UI:      http://localhost:8200" -ForegroundColor White
Write-Host "  - MinIO Console: http://localhost:9001" -ForegroundColor White
Write-Host "`nTo start a service with dev profile:" -ForegroundColor Cyan
Write-Host '  $env:SPRING_PROFILES_ACTIVE="dev"; mvn spring-boot:run' -ForegroundColor White
Write-Host "`n"

