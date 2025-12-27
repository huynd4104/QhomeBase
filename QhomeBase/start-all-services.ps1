# Script de start tat ca microservices cung luc voi log day du
# Usage: .\start-all-services.ps1
# 
# Script nay se:
# 1. Tu dong lay VNPAY_BASE_URL tu ngrok (neu chua set)
# 2. Start tat ca services trong cua so PowerShell rieng de xem log day du

Write-Host "Starting all Qhome Base Microservices..." -ForegroundColor Cyan
Write-Host ""

# Get the script directory
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

# Auto-set VNPAY_BASE_URL from ngrok if not set, otherwise use localhost
if (-not $env:VNPAY_BASE_URL) {
    Write-Host "VNPAY_BASE_URL chua duoc set!" -ForegroundColor Yellow
    Write-Host "   Dang tu dong lay tu ngrok (neu co)..." -ForegroundColor Yellow
    Write-Host ""
    
    $ngrokUrl = $null
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:4040/api/tunnels" -Method Get -ErrorAction Stop
        if ($response.tunnels -and $response.tunnels.Count -gt 0) {
            $httpsTunnel = $response.tunnels | Where-Object { $_.public_url -like "https://*" } | Select-Object -First 1
            if ($httpsTunnel) {
                $ngrokUrl = $httpsTunnel.public_url.TrimEnd('/')
            }
        }
    } catch {
        # Ngrok not running, will use localhost
    }
    
    if ($ngrokUrl) {
        $env:VNPAY_BASE_URL = $ngrokUrl
        Write-Host "Da tu dong set VNPAY_BASE_URL = $env:VNPAY_BASE_URL (tu ngrok)" -ForegroundColor Green
        Write-Host ""
    } else {
        # Lay IP v4 address cua may thay vi localhost
        Write-Host "Ngrok khong chay, dang lay IP v4 address cua may..." -ForegroundColor Yellow
        try {
            $ipAddress = Get-NetIPAddress -AddressFamily IPv4 | 
                Where-Object { $_.IPAddress -notlike "127.*" -and $_.IPAddress -notlike "169.254.*" } | 
                Select-Object -First 1 -ExpandProperty IPAddress
            
            if ($ipAddress) {
                $env:VNPAY_BASE_URL = "http://$ipAddress:8989"
                Write-Host "Da tu dong set VNPAY_BASE_URL = $env:VNPAY_BASE_URL (tu IP v4 address)" -ForegroundColor Green
                Write-Host "   (Neu muon su dung ngrok, chay: ngrok http 8989)" -ForegroundColor Gray
            } else {
                $env:VNPAY_BASE_URL = "http://localhost:8989"
                Write-Host "Khong tim thay IP v4 address, da tu dong set VNPAY_BASE_URL = $env:VNPAY_BASE_URL (localhost)" -ForegroundColor Yellow
                Write-Host "   (Neu muon su dung ngrok, chay: ngrok http 8989 truoc)" -ForegroundColor Gray
            }
        } catch {
            $env:VNPAY_BASE_URL = "http://localhost:8989"
            Write-Host "Loi khi lay IP address, da tu dong set VNPAY_BASE_URL = $env:VNPAY_BASE_URL (localhost)" -ForegroundColor Yellow
            Write-Host "   (Neu muon su dung ngrok, chay: ngrok http 8989 truoc)" -ForegroundColor Gray
        }
        Write-Host ""
    }
} else {
    Write-Host "VNPAY_BASE_URL da duoc set: $env:VNPAY_BASE_URL" -ForegroundColor Green
    Write-Host ""
}

Write-Host ""
Write-Host "Services se duoc start:" -ForegroundColor Cyan
Write-Host "   1. IAM Service (port 8088)" -ForegroundColor White
Write-Host "   2. Base Service (port 8081)" -ForegroundColor White
Write-Host "   3. Customer Interaction Service (port 8086)" -ForegroundColor White
Write-Host "   4. Data Docs Service (port 8082)" -ForegroundColor White
Write-Host "   5. Services Card Service (port 8083)" -ForegroundColor White
Write-Host "   6. Asset Maintenance Service (port 8084)" -ForegroundColor White
Write-Host "   7. Finance Billing Service (port 8085)" -ForegroundColor White
Write-Host "   8. Marketplace Service (port 8089)" -ForegroundColor White
Write-Host "   9. Chat Service (port 8090)" -ForegroundColor White
Write-Host "  10. API Gateway (port 8989)" -ForegroundColor White
Write-Host ""

# Function to start a service with colored output
function Start-ServiceWithLog {
    param(
        [string]$ServiceName,
        [string]$ServicePath,
        [int]$Port,
        [string]$Color = "White"
    )
    
    Write-Host "Starting $ServiceName (port $Port)..." -ForegroundColor $Color
    
    # Start service in new PowerShell window to see logs
    $serviceDir = Join-Path $scriptDir $ServicePath
    $mavenCmd = "mvn spring-boot:run"
    
    Start-Process powershell -ArgumentList @(
        "-NoExit",
        "-Command",
        "cd '$serviceDir'; Write-Host '$ServiceName (port $Port)' -ForegroundColor $Color; Write-Host ''; $mavenCmd"
    ) -WindowStyle Normal
    
    # Small delay between services
    Start-Sleep -Milliseconds 500
}

# Function to start ngrok URL monitor in background
function Start-NgrokMonitor {
    Write-Host ""
    Write-Host "Starting ngrok URL monitor..." -ForegroundColor Cyan
    Write-Host "Monitor se tu dong cap nhat VNPAY_BASE_URL khi ngrok URL thay doi" -ForegroundColor Cyan
    Write-Host "  (Khong tu dong restart - de tu dong restart, chon R)" -ForegroundColor Gray
    
    # Start monitor in background
    Start-Process powershell -ArgumentList @(
        "-NoExit",
        "-Command",
        "cd '$scriptDir'; .\monitor-ngrok-url.ps1"
    ) -WindowStyle Minimized
    
    Write-Host "Ngrok URL monitor da start (chay trong background)" -ForegroundColor Green
}

# Start services in order with proper dependencies
# Order: IAM -> Base -> Customer Interaction -> Others -> Finance Billing -> API Gateway
Write-Host "Starting services..." -ForegroundColor Cyan
Write-Host ""

# Step 1: Start IAM Service first (needed by Base Service)
Start-ServiceWithLog "IAM Service" "iam-service" 8088 "Magenta"
Start-Sleep -Seconds 5  # Wait 5 seconds before next service

# Step 2: Start Base Service (needed by Finance Billing)
Start-ServiceWithLog "Base Service" "base-service" 8081 "Blue"
Start-Sleep -Seconds 5  # Wait 5 seconds before next service

# Step 3: Start Customer Interaction Service (needed by Base Service)
Start-ServiceWithLog "Customer Interaction Service" "customer-interaction-service" 8086 "DarkCyan"
Start-Sleep -Seconds 5  # Wait 5 seconds before next service

# Step 4: Start other independent services
Start-ServiceWithLog "Data Docs Service" "data-docs-service" 8082 "Cyan"
Start-Sleep -Seconds 5  # Wait 5 seconds before next service

Start-ServiceWithLog "Services Card Service" "services-card-service" 8083 "Green"
Start-Sleep -Seconds 5  # Wait 5 seconds before next service

Start-ServiceWithLog "Asset Maintenance Service" "asset-maintenance-service" 8084 "Yellow"
Start-Sleep -Seconds 5  # Wait 5 seconds before next service

# Step 5: Start Finance Billing Service (depends on Base Service)
Start-ServiceWithLog "Finance Billing Service" "finance-billing-service" 8085 "Red"
Start-Sleep -Seconds 5  # Wait 5 seconds before next service

# Step 6: Start Marketplace Service (depends on Base Service for resident info)
Start-ServiceWithLog "Marketplace Service" "marketplace-service" 8089 "DarkGreen"
Start-Sleep -Seconds 5  # Wait 5 seconds before next service

# Step 7: Start Chat Service (depends on Base Service and IAM Service)
Start-ServiceWithLog "Chat Service" "chat-service" 8090 "DarkBlue"
Start-Sleep -Seconds 5  # Wait 5 seconds before next service

# Step 8: Start API Gateway last (routes to all services)
Start-ServiceWithLog "API Gateway" "api-gateway" 8989 "DarkMagenta"

# Step 9: Auto-start ngrok URL monitor based on current VNPAY_BASE_URL
Write-Host ""
$isUsingNgrok = $env:VNPAY_BASE_URL -like "https://*.ngrok*" -or $env:VNPAY_BASE_URL -like "https://*.ngrok-free.app*"

if (-not $isUsingNgrok) {
    # Not using ngrok - auto-start monitor with AutoRestart
    # This ensures that when ngrok starts later, services will automatically restart
    Write-Host "Dang su dung IP address hoac localhost (khong phai ngrok)" -ForegroundColor Yellow
    Write-Host "Tu dong start ngrok URL monitor voi Auto-Restart..." -ForegroundColor Cyan
    Write-Host "  - Khi ngrok start sau nay, services se tu dong restart voi ngrok URL" -ForegroundColor Gray
    Write-Host "  - Khi ngrok dong, services se tu dong restart voi IP address" -ForegroundColor Gray
    Write-Host ""
    
    Start-Process powershell -ArgumentList @(
        "-NoExit",
        "-Command",
        "cd '$scriptDir'; Write-Host 'Starting ngrok URL monitor (Auto-Restart mode)...' -ForegroundColor Cyan; .\monitor-ngrok-url.ps1 -AutoRestart"
    ) -WindowStyle Minimized
    
    Write-Host "Ngrok URL monitor da start (auto-restart mode, chay trong background)" -ForegroundColor Green
    Write-Host "  Monitor se tu dong phat hien khi ngrok start/stop va restart services" -ForegroundColor Gray
} else {
    # Using ngrok - ask user if they want monitor
    Write-Host "Dang su dung ngrok URL: $env:VNPAY_BASE_URL" -ForegroundColor Green
    Write-Host "Ban co muon start ngrok URL monitor?" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  Y = Monitor only:" -ForegroundColor Gray
    Write-Host "     - Tu dong cap nhat VNPAY_BASE_URL khi ngrok URL thay doi" -ForegroundColor Gray
    Write-Host "     - NHUNG services da start se KHONG tu dong nhan URL moi" -ForegroundColor Yellow
    Write-Host "     - Ban can restart services thu cong de nhan URL moi" -ForegroundColor Gray
    Write-Host ""
    Write-Host "  R = Auto-Restart mode (KHUYEN NGHI):" -ForegroundColor Green
    Write-Host "     - Tu dong cap nhat VNPAY_BASE_URL khi ngrok URL thay doi" -ForegroundColor Gray
    Write-Host "     - Tu dong RESTART tat ca services khi URL thay doi" -ForegroundColor Green
    Write-Host "     - Services se TU DONG nhan URL moi ngay lap tuc" -ForegroundColor Green
    Write-Host "     - ✅ NEU services da start voi ngrok URL nay, KHONG CAN RESTART" -ForegroundColor Cyan
    Write-Host "     - Chi restart khi ngrok URL thay doi" -ForegroundColor Gray
    Write-Host ""
    Write-Host "  N = No, khong start monitor" -ForegroundColor Gray
    Write-Host ""
    $startMonitor = Read-Host "  (Y/R/N)"
    if ($startMonitor -eq "Y" -or $startMonitor -eq "y") {
        Start-NgrokMonitor
    } elseif ($startMonitor -eq "R" -or $startMonitor -eq "r") {
        Write-Host ""
        Write-Host "Starting ngrok URL monitor with auto-restart..." -ForegroundColor Cyan
        Write-Host "  Monitor se kiem tra: neu services da start voi ngrok URL nay, se KHONG restart" -ForegroundColor Gray
        Write-Host "  Chi restart khi ngrok URL thay doi" -ForegroundColor Gray
        Write-Host ""
        Start-Process powershell -ArgumentList @(
            "-NoExit",
            "-Command",
            "cd '$scriptDir'; .\monitor-ngrok-url.ps1 -AutoRestart"
        ) -WindowStyle Minimized
        Write-Host "Ngrok URL monitor da start (auto-restart mode, chay trong background)" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "Da start tat ca services!" -ForegroundColor Green
Write-Host ""
Write-Host "Moi service se hien thi log trong cua so PowerShell rieng" -ForegroundColor Cyan
Write-Host ""

# Determine base URL for display
$baseUrl = $env:VNPAY_BASE_URL
$isNgrok = $baseUrl -like "https://*.ngrok*" -or $baseUrl -like "https://*.ngrok-free.app*"

Write-Host "Service URLs:" -ForegroundColor Cyan
if ($isNgrok) {
    # Using ngrok - show ngrok URL for API Gateway, localhost for individual services
    Write-Host "   - API Gateway (Public): $baseUrl" -ForegroundColor Green
    Write-Host "   - API Gateway (Local): http://localhost:8989" -ForegroundColor Gray
    Write-Host ""
    Write-Host "   Individual Services (Local only):" -ForegroundColor Gray
    Write-Host "   - IAM Service: http://localhost:8088" -ForegroundColor White
    Write-Host "   - Base Service: http://localhost:8081" -ForegroundColor White
    Write-Host "   - Customer Interaction: http://localhost:8086" -ForegroundColor White
    Write-Host "   - Data Docs: http://localhost:8082" -ForegroundColor White
    Write-Host "   - Services Card: http://localhost:8083" -ForegroundColor White
    Write-Host "   - Asset Maintenance: http://localhost:8084" -ForegroundColor White
    Write-Host "   - Finance Billing: http://localhost:8085" -ForegroundColor White
    Write-Host "   - Marketplace: http://localhost:8089" -ForegroundColor White
    Write-Host "   - Chat Service: http://localhost:8090" -ForegroundColor White
    Write-Host ""
    Write-Host "   Flutter app should connect to: $baseUrl" -ForegroundColor Cyan
    Write-Host "   VNPay return URLs will use: $baseUrl" -ForegroundColor Cyan
} else {
    # Using local IP or localhost
    Write-Host "   - API Gateway: http://localhost:8989" -ForegroundColor White
    Write-Host "   - IAM Service: http://localhost:8088" -ForegroundColor White
    Write-Host "   - Base Service: http://localhost:8081" -ForegroundColor White
    Write-Host "   - Customer Interaction: http://localhost:8086" -ForegroundColor White
    Write-Host "   - Data Docs: http://localhost:8082" -ForegroundColor White
    Write-Host "   - Services Card: http://localhost:8083" -ForegroundColor White
    Write-Host "   - Asset Maintenance: http://localhost:8084" -ForegroundColor White
    Write-Host "   - Finance Billing: http://localhost:8085" -ForegroundColor White
    Write-Host "   - Marketplace: http://localhost:8089" -ForegroundColor White
    Write-Host "   - Chat Service: http://localhost:8090" -ForegroundColor White
    if ($baseUrl -and $baseUrl -ne "http://localhost:8989") {
        Write-Host ""
        Write-Host "   Flutter app should connect to: $baseUrl" -ForegroundColor Cyan
        Write-Host "   VNPay return URLs will use: $baseUrl" -ForegroundColor Cyan
    }
}
Write-Host ""
Write-Host "Tip: De stop tat ca services, dong cac cua so PowerShell hoac dung Ctrl+C" -ForegroundColor Yellow
Write-Host ""
