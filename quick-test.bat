@echo off
echo Testing application startup...
echo.

echo Building application...
mvn clean compile -q

echo.
echo Starting application (will timeout after 45 seconds)...
timeout /t 45 /nobreak | mvn spring-boot:run

echo.
echo Test completed.
pause
