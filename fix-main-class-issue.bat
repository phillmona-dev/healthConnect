@echo off
echo ========================================
echo   HealthConnect Provider - Fix Main Class Issue
echo ========================================
echo.

echo Step 1: Cleaning project...
call mvn clean
if %ERRORLEVEL% neq 0 (
    echo ERROR: Maven clean failed
    pause
    exit /b 1
)

echo.
echo Step 2: Compiling project...
call mvn compile
if %ERRORLEVEL% neq 0 (
    echo ERROR: Maven compile failed
    pause
    exit /b 1
)

echo.
echo Step 3: Checking if main class exists...
if exist "target\classes\com\medco\HealthConnectProvider\HealthConnectProviderApplication.class" (
    echo SUCCESS: Main class compiled successfully
) else (
    echo ERROR: Main class not found after compilation
    pause
    exit /b 1
)

echo.
echo Step 4: Copying dependencies...
call mvn dependency:copy-dependencies
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to copy dependencies
    pause
    exit /b 1
)

echo.
echo Step 5: Testing main class accessibility...
java -cp "target\classes;target\dependency\*" com.medco.HealthConnectProvider.HealthConnectProviderApplication --help
if %ERRORLEVEL% neq 0 (
    echo ERROR: Main class cannot be executed
    echo This might be due to missing dependencies or database connection issues
) else (
    echo SUCCESS: Main class is accessible
)

echo.
echo Step 6: Building JAR file...
call mvn package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to build JAR
    pause
    exit /b 1
)

echo.
echo ========================================
echo   Fix Complete - Try running the application
echo ========================================
echo.
echo Option 1: Run with Maven
echo   mvn spring-boot:run
echo.
echo Option 2: Run JAR directly
echo   java -jar target\HealthConnectProvider-0.0.1-SNAPSHOT.jar
echo.
echo Option 3: Run with explicit classpath
echo   java -cp "target\classes;target\dependency\*" com.medco.HealthConnectProvider.HealthConnectProviderApplication
echo.
pause
