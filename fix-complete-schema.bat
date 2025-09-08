@echo off
echo ========================================
echo   Complete Schema Fix - Multiple Options
echo ========================================
echo.

echo Choose your preferred approach:
echo.
echo 1. AUTO-GENERATE from JPA Entities (Recommended)
echo    - Generates complete schema from your entities
echo    - Creates proper Liquibase changelogs
echo    - Most reliable and comprehensive
echo.
echo 2. DIFF-BASED Fix (Smart)
echo    - Compares current DB with what it should be
echo    - Generates changelog with only missing parts
echo    - Preserves existing data
echo.
echo 3. MANUAL Complete Fix (Fast)
echo    - Runs comprehensive SQL to add all missing columns
echo    - Quick fix for immediate issues
echo    - Good for development environments
echo.
echo 4. RECREATE Schema (Nuclear option)
echo    - Drops and recreates all tables with complete schema
echo    - Use only if data loss is acceptable
echo    - Fastest but destructive
echo.

set /p CHOICE="Enter your choice (1-4): "

if "%CHOICE%"=="1" goto AUTO_GENERATE
if "%CHOICE%"=="2" goto DIFF_BASED
if "%CHOICE%"=="3" goto MANUAL_FIX
if "%CHOICE%"=="4" goto RECREATE_SCHEMA

echo Invalid choice. Exiting.
pause
exit /b 1

:AUTO_GENERATE
echo.
echo ========================================
echo   Option 1: Auto-Generate from Entities
echo ========================================
echo.
echo This will:
echo 1. Create a temporary database
echo 2. Generate complete schema from your JPA entities
echo 3. Create Liquibase changelogs
echo 4. Provide you with the correct schema
echo.
set /p CONTINUE="Continue with auto-generation? (y/n): "
if /i not "%CONTINUE%"=="y" goto END

call generate-complete-schema.bat
goto END

:DIFF_BASED
echo.
echo ========================================
echo   Option 2: Diff-Based Fix
echo ========================================
echo.
echo This will:
echo 1. Create a reference database with complete schema
echo 2. Compare it with your current database
echo 3. Generate changelog with all missing columns/tables
echo.
set /p CONTINUE="Continue with diff-based fix? (y/n): "
if /i not "%CONTINUE%"=="y" goto END

call generate-diff-changelog.bat
goto END

:MANUAL_FIX
echo.
echo ========================================
echo   Option 3: Manual Complete Fix
echo ========================================
echo.
echo This will run the comprehensive SQL script to add all missing columns.
echo This is safe and preserves existing data.
echo.
set /p CONTINUE="Continue with manual fix? (y/n): "
if /i not "%CONTINUE%"=="y" goto END

echo Running comprehensive SQL fix...
psql -h localhost -p 5432 -U postgres -d healthConnect -f FINAL-FIX-medication-dispensing.sql

if %ERRORLEVEL% equ 0 (
    echo.
    echo ✅ Manual fix completed successfully!
    echo You can now restart your application.
) else (
    echo.
    echo ❌ Error running manual fix. Please check the error messages above.
)
goto END

:RECREATE_SCHEMA
echo.
echo ========================================
echo   Option 4: Recreate Schema (DESTRUCTIVE)
echo ========================================
echo.
echo ⚠️  WARNING: This will drop and recreate tables!
echo ⚠️  You may lose data in non-core tables!
echo ⚠️  Core tables (payers, insured, users) will be backed up.
echo.
set /p CONTINUE="Are you sure you want to recreate the schema? (y/n): "
if /i not "%CONTINUE%"=="y" goto END

echo Backing up core data and recreating schema...
mvn liquibase:update -Dliquibase.changeLogFile=src/main/resources/db/changelog/00-recreate-complete-schema.xml

if %ERRORLEVEL% equ 0 (
    echo.
    echo ✅ Schema recreation completed!
    echo Core data has been backed up.
    echo You can now restart your application.
) else (
    echo.
    echo ❌ Error recreating schema. Please check the error messages above.
)
goto END

:END
echo.
echo ========================================
echo   Next Steps
echo ========================================
echo.
echo After running any of the above options:
echo.
echo 1. 🔄 Restart your application
echo 2. 🧪 Test all endpoints
echo 3. ✅ Verify no more column errors
echo 4. 📝 Update your development process to keep schema in sync
echo.
echo 💡 Pro Tips:
echo - Use Option 1 (Auto-generate) for the most reliable results
echo - Use Option 2 (Diff-based) for production environments
echo - Use Option 3 (Manual) for quick development fixes
echo - Use Option 4 (Recreate) only in development with data loss acceptable
echo.
echo 🎯 To prevent future schema issues:
echo - Always create Liquibase changelogs when modifying entities
echo - Use hibernate.ddl-auto=validate in production
echo - Run schema validation tests in your CI/CD pipeline
echo.

pause
