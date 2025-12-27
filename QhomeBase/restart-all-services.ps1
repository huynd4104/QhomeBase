# Script de restart tat ca services
# Usage: .\restart-all-services.ps1
# 
# Script nay se:
# 1. Tim va dong tat ca cua so PowerShell dang chay services (moi service chi 1 cua so)
# 2. Kill tat ca Java processes cua services
# 3. Doi mot chut de ports duoc giai phong
# 4. Start lai tat ca services voi delay de may khong bi nang

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

Write-Host "Restarting all services..." -ForegroundColor Cyan
Write-Host ""

# Step 0: Auto-detect and set VNPAY_BASE_URL from ngrok (if not already set correctly)
Write-Host "Step 0: Checking VNPAY_BASE_URL..." -ForegroundColor Cyan
Write-Host ""

$shouldUpdateVnpayUrl = $false
$currentVnpayUrl = $env:VNPAY_BASE_URL

# Check if ngrok is running and get URL
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
    # Ngrok not running
}

if ($ngrokUrl) {
    # Ngrok is running
    if ($currentVnpayUrl -ne $ngrokUrl) {
        Write-Host "  [NGROK RUNNING] $ngrokUrl" -ForegroundColor Green
        Write-Host "  [CURRENT VNPAY] $currentVnpayUrl" -ForegroundColor Yellow
        Write-Host "  [UPDATING] VNPAY_BASE_URL to ngrok URL..." -ForegroundColor Cyan
        $env:VNPAY_BASE_URL = $ngrokUrl
        $shouldUpdateVnpayUrl = $true
        Write-Host "  [UPDATED] VNPAY_BASE_URL: $ngrokUrl" -ForegroundColor Green
    } else {
        Write-Host "  [NGROK RUNNING] $ngrokUrl" -ForegroundColor Green
        Write-Host "  [VNPAY OK] Already matches ngrok URL" -ForegroundColor Green
    }
} else {
    # Ngrok not running - use IP address or localhost
    if (-not $currentVnpayUrl -or $currentVnpayUrl -like "*ngrok*") {
        Write-Host "  [NGROK OFF] Not running" -ForegroundColor Gray
        Write-Host "  [SETTING] VNPAY_BASE_URL to IP address..." -ForegroundColor Cyan
        
        try {
            $ipAddress = Get-NetIPAddress -AddressFamily IPv4 | 
                Where-Object { $_.IPAddress -notlike "127.*" -and $_.IPAddress -notlike "169.254.*" } | 
                Select-Object -First 1 -ExpandProperty IPAddress
            
            if ($ipAddress) {
                $env:VNPAY_BASE_URL = "http://$ipAddress:8989"
                Write-Host "  [SET] VNPAY_BASE_URL: $env:VNPAY_BASE_URL" -ForegroundColor Green
            } else {
                $env:VNPAY_BASE_URL = "http://localhost:8989"
                Write-Host "  [LOCALHOST] No IP found, using: $env:VNPAY_BASE_URL" -ForegroundColor Yellow
            }
        } catch {
            $env:VNPAY_BASE_URL = "http://localhost:8989"
            Write-Host "  [LOCALHOST] Error getting IP, using: $env:VNPAY_BASE_URL" -ForegroundColor Yellow
        }
        $shouldUpdateVnpayUrl = $true
    } else {
        Write-Host "  [NGROK OFF] Not running" -ForegroundColor Gray
        Write-Host "  [VNPAY OK] Already set: $currentVnpayUrl" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "  [INFO] Services will use VNPAY_BASE_URL = $($env:VNPAY_BASE_URL)" -ForegroundColor Cyan
Write-Host ""

# Service configuration
$services = @(
    @{Name="IAM Service"; Path="iam-service"; Port=8088; Color="Magenta"},
    @{Name="Base Service"; Path="base-service"; Port=8081; Color="Blue"},
    @{Name="Customer Interaction Service"; Path="customer-interaction-service"; Port=8086; Color="DarkCyan"},
    @{Name="Data Docs Service"; Path="data-docs-service"; Port=8082; Color="Cyan"},
    @{Name="Services Card Service"; Path="services-card-service"; Port=8083; Color="Green"},
    @{Name="Asset Maintenance Service"; Path="asset-maintenance-service"; Port=8084; Color="Yellow"},
    @{Name="Finance Billing Service"; Path="finance-billing-service"; Port=8085; Color="Red"},
    @{Name="Marketplace Service"; Path="marketplace-service"; Port=8089; Color="DarkGreen"},
    @{Name="Chat Service"; Path="chat-service"; Port=8090; Color="DarkBlue"},
    @{Name="API Gateway"; Path="api-gateway"; Port=8989; Color="DarkMagenta"}
)

$killedCount = 0
$closedWindows = 0

Write-Host "Step 1: Closing PowerShell windows running services..." -ForegroundColor Cyan
Write-Host "  (Moi service chi duoc co 1 cua so PowerShell)" -ForegroundColor Gray
Write-Host ""

# Method 1: Close PowerShell windows by finding processes with service paths in command line
# Ensure each service only has 1 PowerShell window
foreach ($service in $services) {
    $servicePath = Join-Path $scriptDir $service.Path
    $serviceName = $service.Name
    $serviceWindows = @()
    
    try {
        # Find all PowerShell processes that might be running this service
        $powershellProcesses = Get-CimInstance Win32_Process -Filter "name = 'powershell.exe'" -ErrorAction SilentlyContinue
        
        foreach ($proc in $powershellProcesses) {
            $processId = $proc.ProcessId
            $cmdLine = $proc.CommandLine
            
            if ($cmdLine -and $cmdLine -like "*$($service.Path)*") {
                $serviceWindows += $processId
            }
        }
    } catch {
        # Try WMI as fallback
        try {
            $powershellProcesses = Get-WmiObject Win32_Process -Filter "name = 'powershell.exe'" -ErrorAction SilentlyContinue
            foreach ($proc in $powershellProcesses) {
                $processId = $proc.ProcessId
                $cmdLine = $proc.CommandLine
                
                if ($cmdLine -and $cmdLine -like "*$($service.Path)*") {
                    $serviceWindows += $processId
                }
            }
        } catch {}
    }
    
    # Close all windows for this service (ensure only 1 window per service)
    if ($serviceWindows.Count -gt 0) {
        Write-Host "  Found $($serviceWindows.Count) PowerShell window(s) for $serviceName" -ForegroundColor Yellow
        foreach ($processId in $serviceWindows) {
            try {
                Write-Host "    Closing window (PID: $processId)..." -ForegroundColor Gray
                Stop-Process -Id $processId -Force -ErrorAction Stop
                $closedWindows++
                Start-Sleep -Milliseconds 200
            } catch {
                # Process might already be closed
            }
        }
    }
}

# Method 2: Kill Java processes by port (to catch any remaining processes)
Write-Host ""
Write-Host "Step 2: Stopping Java processes on service ports..." -ForegroundColor Cyan
Write-Host ""

$servicePorts = @(8088, 8081, 8086, 8082, 8083, 8084, 8085, 8089, 8090, 8989)
$killedProcessIds = @()

foreach ($port in $servicePorts) {
    try {
        $connections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
        if ($connections) {
            $processes = $connections | Select-Object -ExpandProperty OwningProcess -Unique
            
            foreach ($processId in $processes) {
                if ($processId -and $processId -gt 0 -and -not $killedProcessIds.Contains($processId)) {
                    try {
                        $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
                        if ($process) {
                            $processName = $process.ProcessName
                            if ($processName -eq "java" -or $processName -eq "javaw") {
                                Write-Host "  Stopping Java process on port $port (PID: $processId)..." -ForegroundColor Yellow
                                Stop-Process -Id $processId -Force -ErrorAction Stop
                                $killedProcessIds += $processId
                                $killedCount++
                                Start-Sleep -Milliseconds 300
                            }
                        }
                    } catch {
                        # Process might already be stopped
                    }
                }
            }
        }
    } catch {
        # Port might not be in use
    }
}

# Method 3: Kill Java processes by command line (catch any Spring Boot processes)
try {
    $javaProcesses = Get-CimInstance Win32_Process -Filter "name = 'java.exe' OR name = 'javaw.exe'" -ErrorAction SilentlyContinue
    foreach ($proc in $javaProcesses) {
        $processId = $proc.ProcessId
        $cmdLine = $proc.CommandLine
        
        if ($processId -gt 0 -and -not $killedProcessIds.Contains($processId) -and $cmdLine) {
            $isSpringBoot = $cmdLine -like "*spring-boot*" -or 
                           $cmdLine -like "*mvn*" -or 
                           ($cmdLine -like "*qhome*" -or $cmdLine -like "*QhomeBase*") -or
                           $cmdLine -like "*base-service*" -or
                           $cmdLine -like "*finance-billing*" -or
                           $cmdLine -like "*api-gateway*" -or
                           $cmdLine -like "*iam-service*" -or
                           $cmdLine -like "*customer-interaction*" -or
                           $cmdLine -like "*data-docs*" -or
                           $cmdLine -like "*services-card*" -or
                           $cmdLine -like "*asset-maintenance*" -or
                           $cmdLine -like "*marketplace*" -or
                           $cmdLine -like "*chat-service*"
            
            if ($isSpringBoot) {
                try {
                    Write-Host "  Stopping Spring Boot process (PID: $processId)..." -ForegroundColor Yellow
                    Stop-Process -Id $processId -Force -ErrorAction Stop
                    $killedProcessIds += $processId
                    $killedCount++
                    Start-Sleep -Milliseconds 300
                } catch {}
            }
        }
    }
} catch {
    # Try WMI as fallback
    try {
        $javaProcesses = Get-WmiObject Win32_Process -Filter "name = 'java.exe' OR name = 'javaw.exe'" -ErrorAction SilentlyContinue
        foreach ($proc in $javaProcesses) {
            $processId = $proc.ProcessId
            $cmdLine = $proc.CommandLine
            
            if ($processId -gt 0 -and -not $killedProcessIds.Contains($processId) -and $cmdLine) {
                $isSpringBoot = $cmdLine -like "*spring-boot*" -or 
                               $cmdLine -like "*mvn*" -or 
                               ($cmdLine -like "*qhome*" -or $cmdLine -like "*QhomeBase*")
                
                if ($isSpringBoot) {
                    try {
                        Write-Host "  Stopping Spring Boot process (PID: $processId)..." -ForegroundColor Yellow
                        Stop-Process -Id $processId -Force -ErrorAction Stop
                        $killedProcessIds += $processId
                        $killedCount++
                        Start-Sleep -Milliseconds 300
                    } catch {}
                }
            }
        }
    } catch {}
}

# Summary
Write-Host ""
if ($closedWindows -gt 0 -or $killedCount -gt 0) {
    if ($closedWindows -gt 0) {
        Write-Host "Closed $closedWindows PowerShell window(s)" -ForegroundColor Green
    }
    if ($killedCount -gt 0) {
        Write-Host "Stopped $killedCount Java process(es)" -ForegroundColor Green
    }
    
    # Wait for ports to be released
    Write-Host ""
    Write-Host "Step 3: Waiting for ports to be released..." -ForegroundColor Cyan
    Start-Sleep -Seconds 5
    
    # Verify ports are free
    Write-Host "Verifying ports are free..." -ForegroundColor Cyan
    $maxRetries = 3
    $retryCount = 0
    $allPortsFree = $false
    
    while ($retryCount -lt $maxRetries -and -not $allPortsFree) {
        $portsStillInUse = @()
        foreach ($port in $servicePorts) {
            $connections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
            if ($connections) {
                $portsStillInUse += $port
            }
        }
        
        if ($portsStillInUse.Count -eq 0) {
            $allPortsFree = $true
            Write-Host "  All ports are free" -ForegroundColor Green
        } else {
            $retryCount++
            Write-Host "  Warning: Some ports still in use: $($portsStillInUse -join ', ')" -ForegroundColor Yellow
            if ($retryCount -lt $maxRetries) {
                Write-Host "  Retrying... (attempt $retryCount/$maxRetries)" -ForegroundColor Yellow
                Start-Sleep -Seconds 3
                
                # Try to kill processes on those ports again
                foreach ($port in $portsStillInUse) {
                    $connections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
                    if ($connections) {
                        $processes = $connections | Select-Object -ExpandProperty OwningProcess -Unique
                        foreach ($processId in $processes) {
                            try {
                                Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
                            } catch {}
                        }
                    }
                }
            }
        }
    }
    
    if (-not $allPortsFree) {
        Write-Host "  Warning: Some ports may still be in use, but proceeding anyway..." -ForegroundColor Yellow
    }
} else {
    Write-Host "No running services found" -ForegroundColor Yellow
}

# Start all services again
Write-Host ""
Write-Host "Step 4: Starting all services (with delays to reduce system load)..." -ForegroundColor Cyan
Write-Host ""

# Function to start a service with colored output
# This function ensures only 1 window per service by closing old windows first
function Start-ServiceWithLog {
    param(
        [string]$ServiceName,
        [string]$ServicePath,
        [int]$Port,
        [string]$Color = "White",
        [int]$DelaySeconds = 5
    )
    
    # First, ensure no old windows exist for this service
    # Check multiple times to ensure all windows are closed
    $maxCloseAttempts = 3
    $closeAttempt = 0
    
    while ($closeAttempt -lt $maxCloseAttempts) {
        $foundWindows = @()
        
        try {
            # Try CIM first
            $powershellProcesses = Get-CimInstance Win32_Process -Filter "name = 'powershell.exe'" -ErrorAction SilentlyContinue
            foreach ($proc in $powershellProcesses) {
                $processId = $proc.ProcessId
                $cmdLine = $proc.CommandLine
                
                if ($cmdLine -and ($cmdLine -like "*$ServicePath*" -or $cmdLine -like "*$ServiceName*")) {
                    $foundWindows += $processId
                }
            }
        } catch {
            # Try WMI as fallback
            try {
                $powershellProcesses = Get-WmiObject Win32_Process -Filter "name = 'powershell.exe'" -ErrorAction SilentlyContinue
                foreach ($proc in $powershellProcesses) {
                    $processId = $proc.ProcessId
                    $cmdLine = $proc.CommandLine
                    
                    if ($cmdLine -and ($cmdLine -like "*$ServicePath*" -or $cmdLine -like "*$ServiceName*")) {
                        $foundWindows += $processId
                    }
                }
            } catch {}
        }
        
        # Close all found windows
        if ($foundWindows.Count -gt 0) {
            Write-Host "  Found $($foundWindows.Count) old window(s) for $ServiceName, closing..." -ForegroundColor Yellow
            foreach ($processId in $foundWindows) {
                try {
                    Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
                } catch {}
            }
            Start-Sleep -Milliseconds 500
            $closeAttempt++
        } else {
            # No windows found, break
            break
        }
    }
    
    # Additional wait to ensure ports are free
    if ($closeAttempt -gt 0) {
        Write-Host "  Waiting 2 seconds for ports to be released..." -ForegroundColor Gray
        Start-Sleep -Seconds 2
    }
    
    # Double-check port is free before starting
    $portInUse = $false
    try {
        $connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
        if ($connections) {
            $portInUse = $true
            Write-Host "  Warning: Port $Port is still in use, waiting..." -ForegroundColor Yellow
            Start-Sleep -Seconds 3
            
            # Try to kill processes on this port
            $connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
            if ($connections) {
                $processes = $connections | Select-Object -ExpandProperty OwningProcess -Unique
                foreach ($processId in $processes) {
                    try {
                        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
                    } catch {}
                }
                Start-Sleep -Seconds 2
            }
        }
    } catch {}
    
    Write-Host "Starting $ServiceName (port $Port)..." -ForegroundColor $Color
    
    # Start service in new PowerShell window to see logs
    $serviceDir = Join-Path $scriptDir $ServicePath
    $mavenCmd = "mvn spring-boot:run"
    
    # Build command string - properly escape single quotes by doubling them for PowerShell
    $serviceNameEscaped = $ServiceName -replace "'", "''"
    $serviceDirEscaped = $serviceDir -replace "'", "''"
    $portValue = $Port
    $colorValue = $Color
    # Build command using string concatenation to avoid quote escaping issues
    $command = 'cd ''' + $serviceDirEscaped + '''; Write-Host ''' + $serviceNameEscaped + ' (port ' + $portValue + ')' + ''' -ForegroundColor ' + $colorValue + '; Write-Host ''''; ' + $mavenCmd
    
    Start-Process powershell -ArgumentList "-NoExit", "-Command", $command -WindowStyle Normal
    
    # Delay between services to reduce system load
    Write-Host "  Waiting $DelaySeconds seconds before next service..." -ForegroundColor Gray
    Start-Sleep -Seconds $DelaySeconds
}

# Start services in order with proper dependencies and delays (5 seconds between each)
# Step 1: Start IAM Service first (needed by Base Service)
Start-ServiceWithLog "IAM Service" "iam-service" 8088 "Magenta" -DelaySeconds 5
Start-Sleep -Seconds 5  # Wait for IAM to be ready

# Step 2: Start Base Service (needed by Finance Billing)
Start-ServiceWithLog "Base Service" "base-service" 8081 "Blue" -DelaySeconds 5
Start-Sleep -Seconds 8  # Wait for Base Service to be ready

# Step 3: Start Customer Interaction Service (needed by Base Service)
Start-ServiceWithLog "Customer Interaction Service" "customer-interaction-service" 8086 "DarkCyan" -DelaySeconds 5
Start-Sleep -Seconds 5  # Wait for Customer Interaction to be ready

# Step 4: Start other independent services (with 5 second delays)
Start-ServiceWithLog "Data Docs Service" "data-docs-service" 8082 "Cyan" -DelaySeconds 5
Start-ServiceWithLog "Services Card Service" "services-card-service" 8083 "Green" -DelaySeconds 5
Start-ServiceWithLog "Asset Maintenance Service" "asset-maintenance-service" 8084 "Yellow" -DelaySeconds 5

# Step 5: Start Finance Billing Service (depends on Base Service)
Start-Sleep -Seconds 3  # Additional wait before Finance Billing
Start-ServiceWithLog "Finance Billing Service" "finance-billing-service" 8085 "Red" -DelaySeconds 5

# Step 6: Start Marketplace Service (depends on Base Service for resident info)
Start-Sleep -Seconds 3  # Additional wait before Marketplace
Start-ServiceWithLog "Marketplace Service" "marketplace-service" 8089 "DarkGreen" -DelaySeconds 5

# Step 7: Start Chat Service (depends on Base Service and IAM Service)
Start-Sleep -Seconds 3  # Additional wait before Chat Service
Start-ServiceWithLog "Chat Service" "chat-service" 8090 "DarkBlue" -DelaySeconds 5

# Step 8: Start API Gateway last (routes to all services)
Start-Sleep -Seconds 3  # Wait before API Gateway
Start-ServiceWithLog "API Gateway" "api-gateway" 8989 "DarkMagenta" -DelaySeconds 0

Write-Host ""
Write-Host "All services have been restarted!" -ForegroundColor Green
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
        $flutterMsg = "   Flutter app should connect to: $baseUrl"
        $vnpayMsg = "   VNPay return URLs will use: $baseUrl"
        Write-Host $flutterMsg -ForegroundColor Cyan
        Write-Host $vnpayMsg -ForegroundColor Cyan
    }
}
Write-Host ""

Write-Host "Tip: Vui long restart services thu cong neu can: .\restart-all-services.ps1" -ForegroundColor Yellow
Write-Host ""
