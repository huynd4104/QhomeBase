# Script tu dong monitor ngrok URL va cap nhat khi URL thay doi
# Usage: .\monitor-ngrok-url.ps1 [--auto-restart]
# 
# Script nay se:
# 1. Check ngrok URL moi 10 giay
# 2. Neu URL thay doi, tu dong cap nhat VNPAY_BASE_URL
# 3. Neu co flag --auto-restart, tu dong restart services khi URL thay doi
# 4. Chay lien tuc cho den khi nhan Ctrl+C

param(
    [switch]$AutoRestart = $false
)

$PSScriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$lastNgrokUrl = $null
$wasNgrokRunning = $false  # Track if ngrok was running in previous check
$wasUsingNgrok = $false  # Track if we were using ngrok URL before

# Check current VNPAY_BASE_URL to see if services were already started with ngrok URL
$currentVnpayUrl = $env:VNPAY_BASE_URL
$initialCheckDone = $false  # Track if we've done the initial check

Write-Host "Starting ngrok URL monitor..." -ForegroundColor Cyan
Write-Host "Script se tu dong cap nhat VNPAY_BASE_URL khi ngrok URL thay doi" -ForegroundColor Cyan
Write-Host ""

if ($AutoRestart) {
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "AUTO-RESTART MODE ENABLED" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "Khi ngrok URL thay doi:" -ForegroundColor Cyan
    Write-Host "  ✅ Tu dong cap nhat VNPAY_BASE_URL" -ForegroundColor Green
    Write-Host "  ✅ Tu dong RESTART tat ca services" -ForegroundColor Green
    Write-Host "  ✅ Services se TU DONG nhan URL moi" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
} else {
    Write-Host "========================================" -ForegroundColor Yellow
    Write-Host "MONITOR ONLY MODE" -ForegroundColor Yellow
    Write-Host "========================================" -ForegroundColor Yellow
    Write-Host "Khi ngrok URL thay doi:" -ForegroundColor Cyan
    Write-Host "  ✅ Tu dong cap nhat VNPAY_BASE_URL" -ForegroundColor Green
    Write-Host "  ❌ Services da start se KHONG tu dong nhan URL moi" -ForegroundColor Red
    Write-Host "  ⚠️  Ban can restart services thu cong" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "  De bat auto-restart, chay:" -ForegroundColor Cyan
    Write-Host "    .\monitor-ngrok-url.ps1 -AutoRestart" -ForegroundColor White
    Write-Host "========================================" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Nhan Ctrl+C de dung" -ForegroundColor Yellow
Write-Host ""

while ($true) {
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:4040/api/tunnels" -Method Get -ErrorAction Stop
        
        if ($response.tunnels -and $response.tunnels.Count -gt 0) {
            $httpsTunnel = $response.tunnels | Where-Object { $_.public_url -like "https://*" } | Select-Object -First 1
            
            if ($httpsTunnel) {
                $currentNgrokUrl = $httpsTunnel.public_url.TrimEnd('/')
                $isNgrokNowRunning = $true
                
                # Check if ngrok just started (was not running before, now running)
                $ngrokJustStarted = -not $wasNgrokRunning
                
                # Initial check: if VNPAY_BASE_URL already matches ngrok URL, services were started with ngrok
                if (-not $initialCheckDone) {
                    $initialCheckDone = $true
                    if ($currentVnpayUrl -eq $currentNgrokUrl) {
                        # Services were already started with this ngrok URL - no need to restart
                        $lastNgrokUrl = $currentNgrokUrl
                        $wasUsingNgrok = $true
                        $wasNgrokRunning = $true
                        Write-Host "[$(Get-Date -Format 'HH:mm:ss')] ========================================" -ForegroundColor Green
                        Write-Host "[$(Get-Date -Format 'HH:mm:ss')] PHAT HIEN: Services da duoc start voi ngrok URL!" -ForegroundColor Green
                        Write-Host "[$(Get-Date -Format 'HH:mm:ss')] ========================================" -ForegroundColor Green
                        Write-Host "  Ngrok URL: $currentNgrokUrl" -ForegroundColor Green
                        Write-Host "  VNPAY_BASE_URL: $currentVnpayUrl" -ForegroundColor Green
                        Write-Host "  ✅ URL khớp nhau - KHONG CAN RESTART services" -ForegroundColor Green
                        Write-Host "  Monitor se theo doi va chi restart khi URL thay doi" -ForegroundColor Gray
                        Write-Host "[$(Get-Date -Format 'HH:mm:ss')] ========================================" -ForegroundColor Green
                        Write-Host ""
                        continue  # Skip to next iteration
                    }
                }
                
                if ($lastNgrokUrl -eq $null -or $ngrokJustStarted) {
                    # Lan dau tien hoac ngrok vua bat dau - set URL
                    $env:VNPAY_BASE_URL = $currentNgrokUrl
                    $lastNgrokUrl = $currentNgrokUrl
                    $wasUsingNgrok = $true
                    
                    if ($ngrokJustStarted) {
                        Write-Host "[$(Get-Date -Format 'HH:mm:ss')] ========================================" -ForegroundColor Green
                        Write-Host "[$(Get-Date -Format 'HH:mm:ss')] NGROK VUA BAT DAU!" -ForegroundColor Green
                        Write-Host "[$(Get-Date -Format 'HH:mm:ss')] ========================================" -ForegroundColor Green
                        Write-Host "  Ngrok URL: $currentNgrokUrl" -ForegroundColor Green
                        Write-Host "  VNPAY_BASE_URL da duoc cap nhat" -ForegroundColor Green
                        Write-Host ""
                        
                        if ($AutoRestart) {
                            Write-Host "  [AUTO-RESTART] Dang restart tat ca services de nhan ngrok URL..." -ForegroundColor Yellow
                            Write-Host "  Vui long doi, services se tu dong restart..." -ForegroundColor Cyan
                            Write-Host "  (Dang dong cac services cu...)" -ForegroundColor Gray
                            Write-Host ""
                            
                            # Restart all services
                            try {
                                $restartScript = Join-Path $PSScriptRoot "restart-all-services.ps1"
                                & $restartScript
                                
                                Write-Host ""
                                Write-Host "[$(Get-Date -Format 'HH:mm:ss')] ========================================" -ForegroundColor Green
                                Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Services da duoc restart thanh cong!" -ForegroundColor Green
                                Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Tat ca services da nhan ngrok URL: $currentNgrokUrl" -ForegroundColor Green
                                Write-Host "[$(Get-Date -Format 'HH:mm:ss')] ========================================" -ForegroundColor Green
                                Write-Host ""
                            } catch {
                                Write-Host ""
                                Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Loi khi restart services: $($_.Exception.Message)" -ForegroundColor Red
                                Write-Host "  Vui long restart services thu cong: .\restart-all-services.ps1" -ForegroundColor Yellow
                                Write-Host ""
                            }
                        } else {
                            Write-Host "  ⚠️  CANH BAO: Services da start se KHONG tu dong nhan ngrok URL!" -ForegroundColor Red
                            Write-Host "  De services nhan ngrok URL, ban can:" -ForegroundColor Yellow
                            Write-Host "    1. Chay: .\restart-all-services.ps1" -ForegroundColor White
                            Write-Host "    2. Hoac chay: .\monitor-ngrok-url.ps1 -AutoRestart (de tu dong restart)" -ForegroundColor White
                            Write-Host ""
                        }
                        
                        Write-Host "[$(Get-Date -Format 'HH:mm:ss')] ========================================" -ForegroundColor Green
                        Write-Host ""
                    } else {
                        Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Initial ngrok URL: $currentNgrokUrl" -ForegroundColor Green
                        Write-Host "  VNPAY_BASE_URL da duoc set trong session hien tai" -ForegroundColor Gray
                    }
                } elseif ($lastNgrokUrl -ne $currentNgrokUrl) {
                    # URL da thay doi - cap nhat
                    $env:VNPAY_BASE_URL = $currentNgrokUrl
                    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] ========================================" -ForegroundColor Yellow
                    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] NGROK URL DA THAY DOI!" -ForegroundColor Yellow
                    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] ========================================" -ForegroundColor Yellow
                    Write-Host "  Old URL: $lastNgrokUrl" -ForegroundColor Gray
                    Write-Host "  New URL: $currentNgrokUrl" -ForegroundColor Green
                    Write-Host "  VNPAY_BASE_URL da duoc cap nhat trong session hien tai" -ForegroundColor Green
                    Write-Host ""
                    
                    if ($AutoRestart) {
                        Write-Host "  [AUTO-RESTART] Dang restart tat ca services..." -ForegroundColor Yellow
                        Write-Host "  Vui long doi, services se tu dong restart..." -ForegroundColor Cyan
                        Write-Host "  (Dang dong cac services cu...)" -ForegroundColor Gray
                        Write-Host ""
                        
                        # Restart all services
                        try {
                            # Run restart script in same session to ensure environment variable is passed
                            $restartScript = Join-Path $PSScriptRoot "restart-all-services.ps1"
                            & $restartScript
                            
                            Write-Host ""
                            Write-Host "[$(Get-Date -Format 'HH:mm:ss')] ========================================" -ForegroundColor Green
                            Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Services da duoc restart thanh cong!" -ForegroundColor Green
                            Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Tat ca services da nhan URL moi: $currentNgrokUrl" -ForegroundColor Green
                            Write-Host "[$(Get-Date -Format 'HH:mm:ss')] ========================================" -ForegroundColor Green
                            Write-Host ""
                        } catch {
                            Write-Host ""
                            Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Loi khi restart services: $($_.Exception.Message)" -ForegroundColor Red
                            Write-Host "  Vui long restart services thu cong: .\restart-all-services.ps1" -ForegroundColor Yellow
                            Write-Host ""
                        }
                    } else {
                        Write-Host "  ⚠️  CANH BAO: Services da start se KHONG tu dong nhan URL moi!" -ForegroundColor Red
                        Write-Host "  De services nhan URL moi, ban can:" -ForegroundColor Yellow
                        Write-Host "    1. Chay: .\restart-all-services.ps1" -ForegroundColor White
                        Write-Host "    2. Hoac chay: .\monitor-ngrok-url.ps1 -AutoRestart (de tu dong restart)" -ForegroundColor White
                        Write-Host "    3. Hoac restart tung service thu cong" -ForegroundColor White
                    }
                    
                    Write-Host ""
                    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] ========================================" -ForegroundColor Yellow
                    Write-Host ""
                    $lastNgrokUrl = $currentNgrokUrl
                    $wasUsingNgrok = $true
                }
            } else {
                $isNgrokNowRunning = $false
            }
        } else {
            $isNgrokNowRunning = $false
        }
        
        # Update tracking
        $wasNgrokRunning = $isNgrokNowRunning
    } catch {
        # Ngrok chua chay hoac khong the ket noi
        $isNgrokNowRunning = $false
        
        if ($wasNgrokRunning -or $wasUsingNgrok) {
            # Ngrok vua dong hoac da dung ngrok truoc do
            Write-Host "[$(Get-Date -Format 'HH:mm:ss')] ========================================" -ForegroundColor Yellow
            Write-Host "[$(Get-Date -Format 'HH:mm:ss')] NGROK DA DONG!" -ForegroundColor Yellow
            Write-Host "[$(Get-Date -Format 'HH:mm:ss')] ========================================" -ForegroundColor Yellow
            Write-Host "  Ngrok khong con chay, se chuyen sang IP address" -ForegroundColor Yellow
            Write-Host ""
            
            # Lay IP v4 address cua may
            try {
                $ipAddress = Get-NetIPAddress -AddressFamily IPv4 | 
                    Where-Object { $_.IPAddress -notlike "127.*" -and $_.IPAddress -notlike "169.254.*" } | 
                    Select-Object -First 1 -ExpandProperty IPAddress
                
                if ($ipAddress) {
                    $ipUrl = "http://$ipAddress:8989"
                    $env:VNPAY_BASE_URL = $ipUrl
                    Write-Host "  Da chuyen VNPAY_BASE_URL sang IP address: $ipUrl" -ForegroundColor Green
                    Write-Host ""
                    
                    if ($AutoRestart) {
                        Write-Host "  [AUTO-RESTART] Dang restart tat ca services voi IP address..." -ForegroundColor Yellow
                        Write-Host "  Vui long doi, services se tu dong restart..." -ForegroundColor Cyan
                        Write-Host "  (Dang dong cac services cu...)" -ForegroundColor Gray
                        Write-Host ""
                        
                        try {
                            $restartScript = Join-Path $PSScriptRoot "restart-all-services.ps1"
                            & $restartScript
                            
                            Write-Host ""
                            Write-Host "[$(Get-Date -Format 'HH:mm:ss')] ========================================" -ForegroundColor Green
                            Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Services da duoc restart thanh cong!" -ForegroundColor Green
                            Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Tat ca services da chuyen sang IP address: $ipUrl" -ForegroundColor Green
                            Write-Host "[$(Get-Date -Format 'HH:mm:ss')] ========================================" -ForegroundColor Green
                            Write-Host ""
                        } catch {
                            Write-Host ""
                            Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Loi khi restart services: $($_.Exception.Message)" -ForegroundColor Red
                            Write-Host "  Vui long restart services thu cong: .\restart-all-services.ps1" -ForegroundColor Yellow
                            Write-Host ""
                        }
                    } else {
                        Write-Host "  ⚠️  CANH BAO: Services da start se KHONG tu dong nhan IP address moi!" -ForegroundColor Red
                        Write-Host "  De services nhan IP address moi, ban can:" -ForegroundColor Yellow
                        Write-Host "    1. Chay: .\restart-all-services.ps1" -ForegroundColor White
                        Write-Host "    2. Hoac chay: .\monitor-ngrok-url.ps1 -AutoRestart (de tu dong restart)" -ForegroundColor White
                        Write-Host ""
                    }
                } else {
                    # Retry to find IP address - try multiple network interfaces
                    Write-Host "  Khong tim thay IP v4 address, dang thu lai..." -ForegroundColor Yellow
                    $ipAddress = Get-NetIPAddress -AddressFamily IPv4 | 
                        Where-Object { 
                            $_.IPAddress -notlike "127.*" -and 
                            $_.IPAddress -notlike "169.254.*" -and
                            $_.IPAddress -notlike "0.0.0.0"
                        } | 
                        Select-Object -First 1 -ExpandProperty IPAddress
                    
                    if ($ipAddress) {
                        $ipUrl = "http://$ipAddress:8989"
                        $env:VNPAY_BASE_URL = $ipUrl
                        Write-Host "  Tim thay IP v4 address: $ipUrl" -ForegroundColor Green
                    } else {
                        Write-Host "  Khong the tim thay IP v4 address, su dung localhost" -ForegroundColor Yellow
                        Write-Host "  Luu y: localhost chi hoat dong tren cung may, khong hoat dong tren mobile" -ForegroundColor Yellow
                        $env:VNPAY_BASE_URL = "http://localhost:8989"
                    }
                }
            } catch {
                Write-Host "  Loi khi lay IP address: $($_.Exception.Message)" -ForegroundColor Yellow
                Write-Host "  Su dung localhost (chi hoat dong tren cung may)" -ForegroundColor Yellow
                $env:VNPAY_BASE_URL = "http://localhost:8989"
            }
            
            Write-Host "[$(Get-Date -Format 'HH:mm:ss')] ========================================" -ForegroundColor Yellow
            Write-Host ""
        } else {
            Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Ngrok khong chay (chua bat dau)" -ForegroundColor Gray
        }
        
        $wasNgrokRunning = $false
        $wasUsingNgrok = $false
        $lastNgrokUrl = $null
    }
    
    # Doi 10 giay truoc khi check lai
    Start-Sleep -Seconds 10
}
