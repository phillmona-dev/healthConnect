@echo off
echo ========================================
echo   Adding is_cbhi Column (Direct SQL)
echo ========================================
echo.

echo This script will add the is_cbhi column directly using SQL.
echo.

set /p CONTINUE="Do you want to continue? (y/n): "
if /i not "%CONTINUE%"=="y" (
    echo Operation cancelled.
    pause
    exit /b 0
)

echo.
echo Running SQL script to add is_cbhi column...
echo.

psql -h localhost -p 5432 -U postgres -d healthConnect -f scripts/add-is-cbhi-column-direct.sql

if %ERRORLEVEL% equ 0 (
    echo.
    echo ========================================
    echo   is_cbhi Column Added Successfully!
    echo ========================================
    echo.
    echo The is_cbhi column has been added to the payers table.
    echo You can now restart your application.
    echo The error "column p1_0.is_cbhi does not exist" should be resolved.
) else (
    echo.
    echo ========================================
    echo   Error Adding is_cbhi Column
    echo ========================================
    echo.
    echo There was an error running the SQL script.
    echo Please check the error messages above.
    echo.
    echo Alternative: You can run the SQL script manually:
    echo 1. Open pgAdmin or psql
    echo 2. Connect to the healthConnect database
    echo 3. Run the script: scripts/add-is-cbhi-column-direct.sql
)

echo.
pause
