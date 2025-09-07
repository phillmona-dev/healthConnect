@echo off
echo ========================================
echo   Fix All Missing Columns (Complete Solution)
echo ========================================
echo.

echo This script will fix ALL missing column errors:
echo.
echo PAYERS TABLE:
echo - Add is_cbhi column (BOOLEAN, default false)
echo - Add index for performance
echo - Auto-detect CBHI payers by name
echo.
echo MEDICATION_DISPENSING TABLE:
echo - Add attachment_file_name column (VARCHAR 255)
echo - Add attachment_content_type column (VARCHAR 100)
echo - Add attachment_data column (BYTEA)
echo - Add indexes for performance
echo.

echo This will resolve these errors:
echo ❌ "column p1_0.is_cbhi does not exist"
echo ❌ "column md1_0.attachment_content_type does not exist"
echo.

set /p CONTINUE="Do you want to continue? (y/n): "
if /i not "%CONTINUE%"=="y" (
    echo Operation cancelled.
    pause
    exit /b 0
)

echo.
echo Running comprehensive SQL script to fix all missing columns...
echo.

REM Try different PostgreSQL installation paths
set PSQL_PATH=""

REM Check common PostgreSQL installation paths
if exist "C:\Program Files\PostgreSQL\16\bin\psql.exe" (
    set PSQL_PATH="C:\Program Files\PostgreSQL\16\bin\psql.exe"
) else if exist "C:\Program Files\PostgreSQL\15\bin\psql.exe" (
    set PSQL_PATH="C:\Program Files\PostgreSQL\15\bin\psql.exe"
) else if exist "C:\Program Files\PostgreSQL\14\bin\psql.exe" (
    set PSQL_PATH="C:\Program Files\PostgreSQL\14\bin\psql.exe"
) else if exist "C:\Program Files\PostgreSQL\13\bin\psql.exe" (
    set PSQL_PATH="C:\Program Files\PostgreSQL\13\bin\psql.exe"
) else if exist "C:\Program Files (x86)\PostgreSQL\16\bin\psql.exe" (
    set PSQL_PATH="C:\Program Files (x86)\PostgreSQL\16\bin\psql.exe"
) else if exist "C:\Program Files (x86)\PostgreSQL\15\bin\psql.exe" (
    set PSQL_PATH="C:\Program Files (x86)\PostgreSQL\15\bin\psql.exe"
) else (
    echo PostgreSQL psql not found in common installation paths.
    echo Please run the SQL script manually:
    echo.
    echo 1. Open pgAdmin or your PostgreSQL client
    echo 2. Connect to the healthConnect database
    echo 3. Run the script: scripts/fix-all-missing-columns.sql
    echo.
    pause
    exit /b 1
)

echo Found PostgreSQL at: %PSQL_PATH%
echo.

%PSQL_PATH% -h localhost -p 5432 -U postgres -d healthConnect -f scripts/fix-all-missing-columns.sql

if %ERRORLEVEL% equ 0 (
    echo.
    echo ========================================
    echo   ✅ ALL MISSING COLUMNS FIXED SUCCESSFULLY!
    echo ========================================
    echo.
    echo The following columns have been added:
    echo.
    echo PAYERS TABLE:
    echo ✅ is_cbhi column (BOOLEAN, default false)
    echo ✅ Index on is_cbhi for performance
    echo ✅ Auto-detected CBHI payers by name
    echo.
    echo MEDICATION_DISPENSING TABLE:
    echo ✅ attachment_file_name column (VARCHAR 255)
    echo ✅ attachment_content_type column (VARCHAR 100)
    echo ✅ attachment_data column (BYTEA)
    echo ✅ Indexes for performance
    echo.
    echo 🚀 NEXT STEPS:
    echo 1. Restart your HealthConnect application
    echo 2. Test the failing endpoints
    echo 3. Both errors should now be resolved:
    echo    - "column p1_0.is_cbhi does not exist"
    echo    - "column md1_0.attachment_content_type does not exist"
    echo.
    echo Your application should now work without column errors!
) else (
    echo.
    echo ========================================
    echo   ❌ Error Fixing Missing Columns
    echo ========================================
    echo.
    echo There was an error running the SQL script.
    echo Please check the error messages above.
    echo.
    echo MANUAL SOLUTION:
    echo 1. Open pgAdmin or your PostgreSQL client
    echo 2. Connect to the healthConnect database
    echo 3. Run the script: scripts/fix-all-missing-columns.sql
    echo.
    echo QUICK MANUAL FIX:
    echo Run these commands in your PostgreSQL client:
    echo.
    echo -- Fix payers table
    echo ALTER TABLE payers ADD COLUMN IF NOT EXISTS is_cbhi BOOLEAN DEFAULT false NOT NULL;
    echo.
    echo -- Fix medication_dispensing table
    echo ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS attachment_file_name VARCHAR(255);
    echo ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS attachment_content_type VARCHAR(100);
    echo ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS attachment_data BYTEA;
)

echo.
pause
