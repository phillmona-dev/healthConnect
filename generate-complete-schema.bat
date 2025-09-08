@echo off
echo ========================================
echo   Generate Complete Database Schema
echo ========================================
echo.

echo This script will:
echo 1. Create a temporary database 'healthConnect_schema_gen'
echo 2. Generate complete schema from your JPA entities
echo 3. Create Liquibase changelogs from the generated schema
echo 4. Provide you with the complete, correct database structure
echo.

set /p CONTINUE="Do you want to continue? (y/n): "
if /i not "%CONTINUE%"=="y" (
    echo Operation cancelled.
    pause
    exit /b 0
)

echo.
echo Step 1: Creating temporary database for schema generation...
echo.

REM Create temporary database
psql -h localhost -p 5432 -U postgres -c "DROP DATABASE IF EXISTS healthConnect_schema_gen;"
psql -h localhost -p 5432 -U postgres -c "CREATE DATABASE healthConnect_schema_gen;"

if %ERRORLEVEL% neq 0 (
    echo Error creating temporary database. Please ensure PostgreSQL is running.
    pause
    exit /b 1
)

echo.
echo Step 2: Generating complete schema from JPA entities...
echo.

REM Run application with schema generation profile
mvn spring-boot:run -Dspring-boot.run.profiles=schema-gen -Dspring-boot.run.arguments="--spring.config.location=classpath:application-schema-gen.properties"

echo.
echo Step 3: Schema generated! Check target/generated-schema.sql
echo.

echo Step 4: Generating Liquibase changelog from schema...
echo.

REM Generate Liquibase changelog from the generated database
mvn liquibase:generateChangeLog -Dliquibase.outputFile=src/main/resources/db/changelog/00-complete-schema-generated.xml -Dliquibase.url=jdbc:postgresql://localhost:5432/healthConnect_schema_gen -Dliquibase.username=postgres -Dliquibase.password=postgres

echo.
echo Step 5: Cleaning up temporary database...
echo.

REM Clean up temporary database
psql -h localhost -p 5432 -U postgres -c "DROP DATABASE IF EXISTS healthConnect_schema_gen;"

echo.
echo ========================================
echo   Schema Generation Complete!
echo ========================================
echo.
echo Generated files:
echo - target/generated-schema.sql (Complete SQL schema)
echo - src/main/resources/db/changelog/00-complete-schema-generated.xml (Liquibase changelog)
echo.
echo Next steps:
echo 1. Review the generated files
echo 2. Update your db.changelog-master.xml to include the new changelog
echo 3. Apply the changes to your main database
echo.

pause
