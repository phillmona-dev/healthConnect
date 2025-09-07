@echo off
echo ========================================
echo   Adding Attachment Columns (Direct SQL)
echo ========================================
echo.

echo This script will add the missing attachment columns to medication_dispensing table:
echo - attachment_file_name (VARCHAR 255)
echo - attachment_content_type (VARCHAR 100)  
echo - attachment_data (BYTEA)
echo.

set /p CONTINUE="Do you want to continue? (y/n): "
if /i not "%CONTINUE%"=="y" (
    echo Operation cancelled.
    pause
    exit /b 0
)

echo.
echo Running SQL script to add attachment columns...
echo.

psql -h localhost -p 5432 -U postgres -d healthConnect -f scripts/add-attachment-columns-direct.sql

if %ERRORLEVEL% equ 0 (
    echo.
    echo ========================================
    echo   Attachment Columns Added Successfully!
    echo ========================================
    echo.
    echo The following columns have been added to medication_dispensing table:
    echo - attachment_file_name
    echo - attachment_content_type  
    echo - attachment_data
    echo.
    echo You can now restart your application.
    echo The error "column md1_0.attachment_content_type does not exist" should be resolved.
) else (
    echo.
    echo ========================================
    echo   Error Adding Attachment Columns
    echo ========================================
    echo.
    echo There was an error running the SQL script.
    echo Please check the error messages above.
    echo.
    echo Alternative: You can run the SQL script manually:
    echo 1. Open pgAdmin or psql
    echo 2. Connect to the healthConnect database
    echo 3. Run the script: scripts/add-attachment-columns-direct.sql
)

echo.
pause
