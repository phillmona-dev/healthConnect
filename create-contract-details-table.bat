@echo off
echo ========================================
echo   Create Contract Details Table
echo ========================================
echo.

echo This script will:
echo 1. Create the complete contract_details table with ALL columns
echo 2. Add missing columns to existing table (if it exists)
echo 3. Create performance indexes and foreign key constraints
echo 4. Create junction table for many-to-many relationships
echo 5. Verify the table structure
echo.

echo Based on ContractDetail JPA entity with fields:
echo - contract_detail_uuid (unique identifier)
echo - contract_header_uuid (reference to contract header)
echo - service_uuid (reference to service)
echo - negotiated_price (contract price)
echo - status (contract status)
echo - is_deleted (soft delete flag)
echo - drug_uuid (reference to drug)
echo - item_type (type of contract item)
echo - Foreign key relationships to contract_headers, servicelists, drugs
echo - Audit fields (created_by, updated_by, created_at, updated_at)
echo.

set /p CONTINUE="Do you want to create/update the contract_details table? (y/n): "
if /i not "%CONTINUE%"=="y" (
    echo Operation cancelled.
    pause
    exit /b 0
)

echo.
echo Creating contract_details table...
echo.

REM Execute the SQL script
psql -h localhost -p 5432 -U postgres -d healthConnect -f CREATE-contract-details-table.sql

if %ERRORLEVEL% equ 0 (
    echo.
    echo ========================================
    echo   SUCCESS: Contract Details Table Created!
    echo ========================================
    echo.
    echo ✅ contract_details table created with ALL columns
    echo ✅ Performance indexes added for fast queries
    echo ✅ Foreign key constraints established
    echo ✅ Junction table created for many-to-many relationships
    echo ✅ Unique constraints added for data integrity
    echo.
    echo The table now includes all fields from the ContractDetail entity:
    echo - Core fields: contract_detail_uuid, contract_header_uuid, service_uuid
    echo - Business fields: negotiated_price, status, item_type, drug_uuid
    echo - System fields: is_deleted, created_at, updated_at, created_by, updated_by
    echo - Relationship fields: contract_header_id, service_id
    echo.
    echo Next steps:
    echo 1. 🔄 Restart your application
    echo 2. 🧪 Test contract detail operations
    echo 3. ✅ Verify JPA entity mapping works correctly
    echo 4. 📝 Test CRUD operations on contract details
    echo.
) else (
    echo.
    echo ========================================
    echo   ERROR: Failed to create contract_details table
    echo ========================================
    echo.
    echo Please check the error messages above and ensure:
    echo - PostgreSQL is running
    echo - Database 'healthConnect' exists
    echo - User 'postgres' has proper permissions
    echo - No conflicting table structures exist
    echo.
    echo You can also run the SQL script manually:
    echo 1. Open pgAdmin or your PostgreSQL client
    echo 2. Connect to the healthConnect database
    echo 3. Execute the contents of CREATE-contract-details-table.sql
    echo.
)

echo.
echo Alternative: Use Liquibase
echo You can also apply this change using Liquibase:
echo mvn liquibase:update
echo.
echo This will execute the changelog: 14-create-contract-details-table.xml
echo.

pause
