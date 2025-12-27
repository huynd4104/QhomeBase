@echo off
echo Starting Qhome Base Microservices...

echo.
echo Starting Infrastructure Services...
docker-compose -f infra.yml up -d

echo.
echo Waiting for infrastructure to be ready...
timeout /t 30 /nobreak > nul

echo.
echo Starting Application Services...
docker-compose -f apps.yml up -d

echo.
echo All services started successfully!
echo.
echo Service URLs:
echo - API Gateway: http://localhost:8989
echo - Web App: http://localhost:8080
echo - Base Service: http://localhost:8081
echo - Data Docs Service: http://localhost:8082
echo - Services Card Service: http://localhost:8083
echo - Asset Maintenance Service: http://localhost:8084
echo - Finance Billing Service: http://localhost:8085
echo - Customer Interaction Service: http://localhost:8086
echo - Staff Work Service: http://localhost:8087
echo.
echo Infrastructure URLs:
echo - PostgreSQL: localhost:5432
echo - RabbitMQ Management: http://localhost:15672
echo - MailHog: http://localhost:8025
echo - Keycloak: http://localhost:9191
echo.
pause
