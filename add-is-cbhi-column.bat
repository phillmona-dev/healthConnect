@echo off
echo ========================================
echo   Adding is_cbhi Column to Payers Table
echo ========================================
echo.

echo This script will add the missing is_cbhi column to the payers table.
echo.

echo Step 1: Validating Liquibase configuration...
call mvn liquibase:validate
if %ERRORLEVEL% neq 0 (
    echo ERROR: Liquibase validation failed
    pause
    exit /b 1
)
echo ✓ Liquibase configuration is valid

echo.
echo Step 2: Checking current database status...
call mvn liquibase:status
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to check Liquibase status
    pause
    exit /b 1
)
echo ✓ Database status checked

echo.
echo Step 3: Running Liquibase update to add is_cbhi column...
call mvn liquibase:update
if %ERRORLEVEL% neq 0 (
    echo ERROR: Liquibase update failed
    pause
    exit /b 1
)
echo ✓ Liquibase update completed

echo.
echo ========================================
echo   is_cbhi Column Added Successfully!
echo ========================================
echo.
echo The following changes have been applied:
echo ✓ Added is_cbhi column to payers table
echo ✓ Set default value to false for existing records
echo ✓ Added not null constraint
echo ✓ Created performance indexes
echo ✓ Updated CBHI payers based on naming conventions
echo.
echo You can now restart your application.
echo The error "column p1_0.is_cbhi does not exist" should be resolved.
echo.
pause
