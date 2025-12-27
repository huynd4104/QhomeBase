# Script đơn giản để chạy tất cả services cùng lúc
# Usage: .\run-all-services.ps1
# 
# Script này sẽ chạy tất cả services trong các cửa sổ PowerShell riêng biệt

Write-Host "🚀 Starting all Qhome Base Microservices..." -ForegroundColor Cyan
Write-Host ""

# Tự động cd vào thư mục chứa script
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $scriptDir) {
    $scriptDir = $PSScriptRoot
}
if (-not $scriptDir) {
    $scriptDir = Get-Location
}

Set-Location $scriptDir
Write-Host "Working directory: $scriptDir" -ForegroundColor Gray
Write-Host ""

# Danh sách services
$services = @(
    @{Name="IAM Service"; Path="iam-service"; Port=8088; Color="Magenta"},
    @{Name="Base Service"; Path="base-service"; Port=8081; Color="Blue"},
    @{Name="Customer Interaction Service"; Path="customer-interaction-service"; Port=8086; Color="DarkCyan"},
    @{Name="Data Docs Service"; Path="data-docs-service"; Port=8082; Color="Cyan"},
    @{Name="Services Card Service"; Path="services-card-service"; Port=8083; Color="Green"},
    @{Name="Asset Maintenance Service"; Path="asset-maintenance-service"; Port=8084; Color="Yellow"},
    @{Name="Finance Billing Service"; Path="finance-billing-service"; Port=8085; Color="Red"},
    @{Name="API Gateway"; Path="api-gateway"; Port=8989; Color="DarkMagenta"}
)

Write-Host "Services sẽ được start:" -ForegroundColor Cyan
foreach ($service in $services) {
    Write-Host "   - $($service.Name) (port $($service.Port))" -ForegroundColor White
}
Write-Host ""

# Function để start service
function Start-Service {
    param(
        [string]$ServiceName,
        [string]$ServicePath,
        [int]$Port,
        [string]$Color = "White"
    )
    
    Write-Host "Starting $ServiceName (port $Port)..." -ForegroundColor $Color
    
    $serviceDir = Join-Path $scriptDir $ServicePath
    
    # Đảm bảo đường dẫn có định dạng đúng cho PowerShell
    $serviceDirFull = (Resolve-Path $serviceDir -ErrorAction SilentlyContinue).Path
    if (-not $serviceDirFull) {
        $serviceDirFull = $serviceDir
    }
    
    Start-Process powershell -ArgumentList @(
        "-NoExit",
        "-Command",
        "cd '$serviceDirFull'; Write-Host '$ServiceName (port $Port)' -ForegroundColor $Color; Write-Host ''; Write-Host 'Current directory:' -ForegroundColor Gray; pwd; Write-Host ''; mvn spring-boot:run"
    ) -WindowStyle Normal
    
    Start-Sleep -Milliseconds 500
}

# Start tất cả services
Write-Host "Starting services..." -ForegroundColor Cyan
Write-Host ""

foreach ($service in $services) {
    Start-Service -ServiceName $service.Name -ServicePath $service.Path -Port $service.Port -Color $service.Color
    Start-Sleep -Seconds 3
}

Write-Host ""
Write-Host "✅ Đã start tất cả services!" -ForegroundColor Green
Write-Host ""
Write-Host "Service URLs:" -ForegroundColor Cyan
Write-Host "   - API Gateway: http://localhost:8989" -ForegroundColor White
Write-Host "   - IAM Service: http://localhost:8088" -ForegroundColor White
Write-Host "   - Base Service: http://localhost:8081" -ForegroundColor White
Write-Host "   - Customer Interaction: http://localhost:8086" -ForegroundColor White
Write-Host "   - Data Docs: http://localhost:8082" -ForegroundColor White
Write-Host "   - Services Card: http://localhost:8083" -ForegroundColor White
Write-Host "   - Asset Maintenance: http://localhost:8084" -ForegroundColor White
Write-Host "   - Finance Billing: http://localhost:8085" -ForegroundColor White
Write-Host ""
Write-Host "💡 Tip: Để stop tất cả services, đóng các cửa sổ PowerShell hoặc dùng Ctrl+C" -ForegroundColor Yellow
Write-Host ""

