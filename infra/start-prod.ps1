<#
.SYNOPSIS
    Start Production Environment
.DESCRIPTION
    Starts all infrastructure services with production configuration
    WARNING: This is for reference only. In production, use Kubernetes or Docker Swarm.
#>

Write-Host "`n=== Starting PRODUCTION Environment ===" -ForegroundColor Red

Write-Host @"

WARNING: This script is for REFERENCE ONLY!

In production, you should:
1. Use Kubernetes with Helm charts
2. Or Docker Swarm with proper secrets management
3. Use managed services (AWS RDS, Azure CosmosDB, etc.)
4. Configure proper TLS/SSL certificates
5. Set up monitoring and alerting

"@ -ForegroundColor Yellow

$confirm = Read-Host "Are you sure you want to continue? (yes/no)"
if ($confirm -ne "yes") {
    Write-Host "Aborted." -ForegroundColor Yellow
    exit 0
}

# Check required environment variables
$required = @("POSTGRES_USER", "POSTGRES_PASSWORD", "MONGO_USER", "MONGO_PASSWORD", 
              "REDIS_PASSWORD", "MINIO_USER", "MINIO_PASSWORD", "VAULT_ROLE_ID", "VAULT_SECRET_ID")

$missing = @()
foreach ($var in $required) {
    if (-not (Get-Item "env:$var" -ErrorAction SilentlyContinue)) {
        $missing += $var
    }
}

if ($missing.Count -gt 0) {
    Write-Host "ERROR: Missing required environment variables:" -ForegroundColor Red
    $missing | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
    exit 1
}

# Set production environment
$env:SPRING_PROFILES_ACTIVE = "prod"

# Start infrastructure with production config
Write-Host "`nStarting infrastructure services (production mode)..." -ForegroundColor Yellow
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

Write-Host "`n=== Production Environment Started ===" -ForegroundColor Green
Write-Host "`nIMPORTANT: Remember to:" -ForegroundColor Yellow
Write-Host "  1. Unseal Vault manually" -ForegroundColor White
Write-Host "  2. Configure backup jobs" -ForegroundColor White
Write-Host "  3. Set up monitoring" -ForegroundColor White
Write-Host "`n"

