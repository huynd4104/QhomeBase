# Script tu dong lay URL tu ngrok va set VNPAY_BASE_URL
# Usage: .\auto-set-vnpay-from-ngrok.ps1
# 
# Neu ngrok dang chay: se lay ngrok URL
# Neu ngrok khong chay: se set localhost URL (http://localhost:8989)
# Sau khi set VNPAY_BASE_URL, script se hoi co muon start tat ca services khong
# Hoac co the start thu cong bang: .\start-all-services.ps1

# Get script directory for calling other scripts
$PSScriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "Dang kiem tra ngrok..." -ForegroundColor Cyan

$ngrokUrl = $null
$isLocalhost = $false

try {
    # Lay URL tu ngrok API (localhost:4040)
    $response = Invoke-RestMethod -Uri "http://localhost:4040/api/tunnels" -Method Get -ErrorAction Stop
    
    if ($response.tunnels -and $response.tunnels.Count -gt 0) {
        # Tim tunnel co public_url (HTTPS)
        $httpsTunnel = $response.tunnels | Where-Object { $_.public_url -like "https://*" } | Select-Object -First 1
        
        if ($httpsTunnel) {
            $ngrokUrl = $httpsTunnel.public_url.TrimEnd('/')
            Write-Host "Tim thay ngrok URL: $ngrokUrl" -ForegroundColor Green
        } else {
            Write-Host "Khong tim thay HTTPS tunnel trong ngrok" -ForegroundColor Yellow
        }
    } else {
        Write-Host "Khong tim thay tunnel nao trong ngrok" -ForegroundColor Yellow
    }
} catch {
    Write-Host "Ngrok khong dang chay hoac khong the ket noi (localhost:4040)" -ForegroundColor Yellow
    Write-Host "   Se su dung localhost URL thay vi ngrok URL" -ForegroundColor Cyan
}

# Neu khong co ngrok URL, lay IP v4 address cua may
if (-not $ngrokUrl) {
    Write-Host ""
    Write-Host "Dang lay IP v4 address cua may..." -ForegroundColor Cyan
    
    try {
        # Lay IP v4 address (khong phai loopback)
        $ipAddress = Get-NetIPAddress -AddressFamily IPv4 | 
            Where-Object { $_.IPAddress -notlike "127.*" -and $_.IPAddress -notlike "169.254.*" } | 
            Select-Object -First 1 -ExpandProperty IPAddress
        
        if ($ipAddress) {
            $ngrokUrl = "http://$ipAddress:8989"
            $isLocalhost = $false
            Write-Host "Tim thay IP v4 address: $ipAddress" -ForegroundColor Green
            Write-Host "Su dung IP URL: $ngrokUrl" -ForegroundColor Green
            Write-Host "   (Neu muon su dung ngrok, chay: ngrok http 8989)" -ForegroundColor Gray
        } else {
            # Fallback to localhost if cannot get IP
            $ngrokUrl = "http://localhost:8989"
            $isLocalhost = $true
            Write-Host "Khong tim thay IP v4 address, su dung localhost: $ngrokUrl" -ForegroundColor Yellow
            Write-Host "   (Neu muon su dung ngrok, chay: ngrok http 8989 truoc)" -ForegroundColor Gray
        }
    } catch {
        # Fallback to localhost if error getting IP
        $ngrokUrl = "http://localhost:8989"
        $isLocalhost = $true
        Write-Host "Loi khi lay IP address, su dung localhost: $ngrokUrl" -ForegroundColor Yellow
        Write-Host "   (Neu muon su dung ngrok, chay: ngrok http 8989 truoc)" -ForegroundColor Gray
    }
} else {
    Write-Host ""
    Write-Host "Su dung ngrok URL: $ngrokUrl" -ForegroundColor Green
}

# Set environment variable
$env:VNPAY_BASE_URL = $ngrokUrl

Write-Host ""
Write-Host "Da tu dong set VNPAY_BASE_URL = $ngrokUrl" -ForegroundColor Green
Write-Host ""
Write-Host "Cac services se tu dong dung return URLs:" -ForegroundColor Cyan
Write-Host "  - Finance Billing: $ngrokUrl/api/invoices/vnpay/redirect" -ForegroundColor Yellow
Write-Host "  - Services Card (Vehicle): $ngrokUrl/api/register-service/vnpay/redirect" -ForegroundColor Yellow
Write-Host "  - Services Card (Resident): $ngrokUrl/api/resident-card/vnpay/redirect" -ForegroundColor Yellow
Write-Host "  - Services Card (Elevator): $ngrokUrl/api/elevator-card/vnpay/redirect" -ForegroundColor Yellow
Write-Host "  - Asset Maintenance: $ngrokUrl/api/asset-maintenance/bookings/vnpay/redirect" -ForegroundColor Yellow
Write-Host ""

if ($isLocalhost) {
    Write-Host "Luu y: Voi localhost URL, VNPay se khong the redirect ve backend tu internet" -ForegroundColor Yellow
    Write-Host "   Chi phu hop cho development local, khong phu hop cho production" -ForegroundColor Yellow
    Write-Host ""
} else {
    Write-Host "Tip: De tu dong cap nhat khi ngrok URL thay doi, chay:" -ForegroundColor Cyan
    Write-Host "   .\monitor-ngrok-url.ps1" -ForegroundColor White
    Write-Host "   Hoac chon Y khi start services de tu dong bat monitor" -ForegroundColor Cyan
    Write-Host ""
}

# Ask if user wants to start all services
Write-Host "Ban co muon start tat ca services ngay bay gio?" -ForegroundColor Cyan
if ($isLocalhost) {
    Write-Host "  (Neu sau nay chay ngrok, services se tu dong restart voi ngrok URL)" -ForegroundColor Gray
}
$startServices = Read-Host "   (Y = Yes, N = No, chi set VNPAY_BASE_URL)"
if ($startServices -eq "Y" -or $startServices -eq "y") {
    Write-Host ""
    Write-Host "Starting all services..." -ForegroundColor Cyan
    & "$PSScriptRoot\start-all-services.ps1"
} else {
    Write-Host ""
    Write-Host "De start services sau, chay:" -ForegroundColor Cyan
    Write-Host "   .\start-all-services.ps1" -ForegroundColor White
    if ($isLocalhost) {
        Write-Host ""
        Write-Host "Tip: Khi start services, monitor se tu dong start de phat hien ngrok" -ForegroundColor Cyan
    }
}
