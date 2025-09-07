@echo off
echo ========================================
echo   Adding Missing Claim Tables
echo ========================================
echo.

echo The following tables will be created:
echo - BatchLog
echo - BatchRecord  
echo - ClaimAttachment
echo - ClaimComment
echo - ClaimItem
echo - ClaimLogs
echo - ClaimPayment
echo.

echo Step 1: Validating Liquibase configuration...
call mvn liquibase:validate
if %ERRORLEVEL% neq 0 (
    echo ERROR: Liquibase validation failed
    pause
    exit /b 1
)

echo.
echo Step 2: Checking current database status...
call mvn liquibase:status
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to check Liquibase status
    pause
    exit /b 1
)

echo.
echo Step 3: Running Liquibase update to create missing tables...
call mvn liquibase:update
if %ERRORLEVEL% neq 0 (
    echo ERROR: Liquibase update failed
    pause
    exit /b 1
)

echo.
echo ========================================
echo   Missing Tables Added Successfully!
echo ========================================
echo.
echo The following tables have been created:
echo ✓ batch_records
echo ✓ batch_logs  
echo ✓ claim_attachments
echo ✓ claim_comments
echo ✓ claim_items
echo ✓ claim_logs
echo ✓ claim_payments
echo.
echo All foreign key relationships have been established.
echo.
echo You can now restart your application to use the new tables.
echo.
pause
