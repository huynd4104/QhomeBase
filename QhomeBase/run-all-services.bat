@echo off
REM Script đơn giản để chạy tất cả services cùng lúc
REM Usage: run-all-services.bat

echo Starting all Qhome Base Microservices...
echo.

REM Lấy thư mục chứa script và cd vào đó
cd /d "%~dp0"
set "BASE_DIR=%~dp0"

REM Start IAM Service
start "IAM Service (8088)" cmd /k "cd /d "%BASE_DIR%iam-service" && mvn spring-boot:run"

REM Start Base Service
timeout /t 3 /nobreak >nul
start "Base Service (8081)" cmd /k "cd /d "%BASE_DIR%base-service" && mvn spring-boot:run"

REM Start Customer Interaction Service
timeout /t 3 /nobreak >nul
start "Customer Interaction Service (8086)" cmd /k "cd /d "%BASE_DIR%customer-interaction-service" && mvn spring-boot:run"

REM Start Data Docs Service
timeout /t 3 /nobreak >nul
start "Data Docs Service (8082)" cmd /k "cd /d "%BASE_DIR%data-docs-service" && mvn spring-boot:run"

REM Start Services Card Service
timeout /t 3 /nobreak >nul
start "Services Card Service (8083)" cmd /k "cd /d "%BASE_DIR%services-card-service" && mvn spring-boot:run"

REM Start Asset Maintenance Service
timeout /t 3 /nobreak >nul
start "Asset Maintenance Service (8084)" cmd /k "cd /d "%BASE_DIR%asset-maintenance-service" && mvn spring-boot:run"

REM Start Finance Billing Service
timeout /t 3 /nobreak >nul
start "Finance Billing Service (8085)" cmd /k "cd /d "%BASE_DIR%finance-billing-service" && mvn spring-boot:run"

REM Start API Gateway
timeout /t 3 /nobreak >nul
start "API Gateway (8989)" cmd /k "cd /d "%BASE_DIR%api-gateway" && mvn spring-boot:run"

echo.
echo All services have been started!
echo.
echo Service URLs:
echo    - API Gateway: http://localhost:8989
echo    - IAM Service: http://localhost:8088
echo    - Base Service: http://localhost:8081
echo    - Customer Interaction: http://localhost:8086
echo    - Data Docs: http://localhost:8082
echo    - Services Card: http://localhost:8083
echo    - Asset Maintenance: http://localhost:8084
echo    - Finance Billing: http://localhost:8085
echo.
pause

