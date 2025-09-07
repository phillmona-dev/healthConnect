@echo off
echo ========================================
echo   Complete Medication Dispensing Table Fix
echo ========================================
echo.

echo This script will add ALL missing columns to medication_dispensing table:
echo.
echo CORE COLUMNS:
echo - invoice_number, batch_code, dispensing_uuid
echo - provider_uuid, payer_uuid, insured_uuid
echo - prescription_number, pharmacy_transaction_id
echo - dispensing_date, prescribing_physician_name
echo - prescribing_physician_id, recorded_at
echo - branch_name, claim_status, remark, status, claim_uuid
echo.
echo FINANCIAL COLUMNS:
echo - total_amount, patient_responsibility, insurance_coverage
echo - pharmacist_notes, source
echo.
echo SYSTEM COLUMNS:
echo - batch_id, insured_id, dependant_id
echo - deleted, created_at, updated_at
echo.
echo DIAGNOSIS COLUMNS:
echo - primary_diagnosis, secondary_diagnosis
echo.
echo ATTACHMENT COLUMNS:
echo - attachment_file_name, attachment_content_type, attachment_data
echo.
echo INDEXES AND CONSTRAINTS:
echo - Unique constraints on dispensing_uuid and pharmacy_transaction_id
echo - Performance indexes on all key columns
echo.

echo This will resolve ALL column errors like:
echo ❌ "column md1_0.batch_code does not exist"
echo ❌ "column md1_0.attachment_content_type does not exist"
echo ❌ And any other missing column errors
echo.

set /p CONTINUE="Do you want to continue? (y/n): "
if /i not "%CONTINUE%"=="y" (
    echo Operation cancelled.
    pause
    exit /b 0
)

echo.
echo Running comprehensive SQL script to complete medication_dispensing table...
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
    echo 3. Run the script: scripts/complete-medication-dispensing-table.sql
    echo.
    pause
    exit /b 1
)

echo Found PostgreSQL at: %PSQL_PATH%
echo.

%PSQL_PATH% -h localhost -p 5432 -U postgres -d healthConnect -f scripts/complete-medication-dispensing-table.sql

if %ERRORLEVEL% equ 0 (
    echo.
    echo ========================================
    echo   ✅ MEDICATION_DISPENSING TABLE COMPLETED!
    echo ========================================
    echo.
    echo The following has been accomplished:
    echo.
    echo ✅ ALL MISSING COLUMNS ADDED:
    echo   - Core dispensing columns (invoice_number, batch_code, etc.)
    echo   - Financial columns (total_amount, patient_responsibility, etc.)
    echo   - System columns (deleted, created_at, updated_at)
    echo   - Diagnosis columns (primary_diagnosis, secondary_diagnosis)
    echo   - Attachment columns (attachment_file_name, attachment_content_type, attachment_data)
    echo   - Foreign key columns (batch_id, insured_id, dependant_id)
    echo.
    echo ✅ PERFORMANCE OPTIMIZATIONS:
    echo   - Unique constraints on dispensing_uuid and pharmacy_transaction_id
    echo   - Indexes on all key columns for fast queries
    echo.
    echo 🚀 NEXT STEPS:
    echo 1. Restart your HealthConnect application
    echo 2. Test the failing endpoints
    echo 3. ALL column errors should now be resolved:
    echo    - "column md1_0.batch_code does not exist"
    echo    - "column md1_0.attachment_content_type does not exist"
    echo    - Any other missing column errors
    echo.
    echo Your medication dispensing functionality should now work perfectly!
) else (
    echo.
    echo ========================================
    echo   ❌ Error Completing Medication Dispensing Table
    echo ========================================
    echo.
    echo There was an error running the SQL script.
    echo Please check the error messages above.
    echo.
    echo MANUAL SOLUTION:
    echo 1. Open pgAdmin or your PostgreSQL client
    echo 2. Connect to the healthConnect database
    echo 3. Run the script: scripts/complete-medication-dispensing-table.sql
    echo.
    echo QUICK MANUAL FIX (run these in your PostgreSQL client):
    echo ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS batch_code VARCHAR(255);
    echo ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS attachment_content_type VARCHAR(100);
    echo ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS attachment_file_name VARCHAR(255);
    echo ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS attachment_data BYTEA;
    echo -- (and all other missing columns as shown in the script)
)

echo.
pause
