@echo off
echo Starting HealthConnect Provider Application...
echo.

echo Checking if main class exists...
if exist "target\classes\com\medco\HealthConnectProvider\HealthConnectProviderApplication.class" (
    echo Main class found!
) else (
    echo Main class not found. Compiling...
    mvn clean compile
)

echo.
echo Starting application with Maven...
mvn spring-boot:run

pause
