@echo off
echo Stopping Qhome Base Microservices...

echo.
echo Stopping Application Services...
docker-compose -f apps.yml down

echo.
echo Stopping Infrastructure Services...
docker-compose -f infra.yml down

echo.
echo All services stopped successfully!
echo.
pause
