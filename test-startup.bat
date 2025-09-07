@echo off
echo ========================================
echo   Testing Application Startup
echo ========================================
echo.

echo Step 1: Compiling application...
call mvn compile -q
if %ERRORLEVEL% neq 0 (
    echo ERROR: Compilation failed
    pause
    exit /b 1
)
echo ✓ Compilation successful

echo.
echo Step 2: Building JAR file...
call mvn package -DskipTests -q
if %ERRORLEVEL% neq 0 (
    echo ERROR: Build failed
    pause
    exit /b 1
)
echo ✓ Build successful

echo.
echo Step 3: Starting application (will run for 30 seconds)...
echo Press Ctrl+C to stop if needed
echo.

timeout /t 3 /nobreak > nul

start /b java -jar target/HealthConnectProvider-0.0.1-SNAPSHOT.jar > startup.log 2>&1

echo Waiting for application to start...
timeout /t 30 /nobreak > nul

echo.
echo ========================================
echo   Startup Test Results
echo ========================================
echo.

if exist startup.log (
    echo Checking startup log for errors...
    findstr /i "error exception failed" startup.log > nul
    if %ERRORLEVEL% equ 0 (
        echo ❌ ERRORS FOUND in startup log:
        echo.
        findstr /i "error exception failed" startup.log
    ) else (
        echo ✓ No critical errors found in startup log
    )
    
    echo.
    echo Checking if application started successfully...
    findstr /i "started.*in.*seconds" startup.log > nul
    if %ERRORLEVEL% equ 0 (
        echo ✓ Application started successfully:
        findstr /i "started.*in.*seconds" startup.log
    ) else (
        echo ❌ Application may not have started properly
    )
    
    echo.
    echo Last 10 lines of startup log:
    echo ----------------------------------------
    powershell "Get-Content startup.log | Select-Object -Last 10"
    echo ----------------------------------------
) else (
    echo ❌ No startup log found
)

echo.
echo Stopping application...
taskkill /f /im java.exe > nul 2>&1

echo.
echo Test complete. Check startup.log for full details.
pause
