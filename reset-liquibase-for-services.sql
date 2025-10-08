-- ========================================
-- Reset Liquibase to Create Services Table
-- ========================================
-- Run this SQL in pgAdmin before restarting your application

-- Step 1: Clear all Liquibase tracking
-- This will make Liquibase start fresh with only the new changelog
TRUNCATE TABLE databasechangelog;
TRUNCATE TABLE databasechangeloglock;

-- Step 2: Verify the tables are cleared
SELECT COUNT(*) as remaining_changesets FROM databasechangelog;

-- Step 3: Check if services table exists
SELECT 
    CASE 
        WHEN EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'services')
        THEN 'services table EXISTS'
        ELSE 'services table DOES NOT EXIST'
    END as table_status;

-- Step 4: If services table exists but you want to recreate it, uncomment this:
-- DROP TABLE IF EXISTS services CASCADE;

-- ========================================
-- After running this SQL:
-- 1. Restart your Spring Boot application
-- 2. Liquibase will run the new changelog
-- 3. Services table will be created
-- ========================================

