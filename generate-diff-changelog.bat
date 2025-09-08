@echo off
echo ========================================
echo   Generate Diff Changelog (Missing Columns)
echo ========================================
echo.

echo This script will:
echo 1. Compare your current database with what it should be (based on entities)
echo 2. Generate a changelog with all missing columns and tables
echo 3. Create a comprehensive fix for all schema mismatches
echo.

set /p CONTINUE="Do you want to continue? (y/n): "
if /i not "%CONTINUE%"=="y" (
    echo Operation cancelled.
    pause
    exit /b 0
)

echo.
echo Step 1: Creating reference database with complete schema...
echo.

REM Create reference database
psql -h localhost -p 5432 -U postgres -c "DROP DATABASE IF EXISTS healthConnect_reference;"
psql -h localhost -p 5432 -U postgres -c "CREATE DATABASE healthConnect_reference;"

if %ERRORLEVEL% neq 0 (
    echo Error creating reference database. Please ensure PostgreSQL is running.
    pause
    exit /b 1
)

echo.
echo Step 2: Generating complete schema in reference database...
echo.

REM Generate complete schema in reference database
mvn spring-boot:run -Dspring-boot.run.profiles=schema-gen -Dspring-boot.run.arguments="--spring.config.location=classpath:application-schema-gen.properties --spring.datasource.url=jdbc:postgresql://localhost:5432/healthConnect_reference"

echo.
echo Step 3: Generating diff changelog between current and reference database...
echo.

REM Generate diff changelog
mvn liquibase:diff -Dliquibase.referenceUrl=jdbc:postgresql://localhost:5432/healthConnect_reference -Dliquibase.referenceUsername=postgres -Dliquibase.referencePassword=postgres -Dliquibase.url=jdbc:postgresql://localhost:5432/healthConnect -Dliquibase.username=postgres -Dliquibase.password=postgres

echo.
echo Step 4: Generating diff changelog file...
echo.

REM Generate diff changelog file
mvn liquibase:diffChangeLog -Dliquibase.referenceUrl=jdbc:postgresql://localhost:5432/healthConnect_reference -Dliquibase.referenceUsername=postgres -Dliquibase.referencePassword=postgres -Dliquibase.url=jdbc:postgresql://localhost:5432/healthConnect -Dliquibase.username=postgres -Dliquibase.password=postgres -Dliquibase.diffChangeLogFile=src/main/resources/db/changelog/99-missing-columns-fix.xml

echo.
echo Step 5: Cleaning up reference database...
echo.

REM Clean up reference database
psql -h localhost -p 5432 -U postgres -c "DROP DATABASE IF EXISTS healthConnect_reference;"

echo.
echo ========================================
echo   Diff Changelog Generation Complete!
echo ========================================
echo.
echo Generated file:
echo - src/main/resources/db/changelog/99-missing-columns-fix.xml
echo.
echo This changelog contains ALL missing columns and tables!
echo.
echo Next steps:
echo 1. Review the generated changelog
echo 2. Add it to your db.changelog-master.xml
echo 3. Run mvn liquibase:update to apply all missing changes
echo.

pause
