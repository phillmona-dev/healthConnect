-- ========================================
-- Fix Liquibase Tracking for Missing Tables
-- ========================================
-- Run this SQL in pgAdmin before restarting your application

-- Step 1: Check which tables exist
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
  AND table_type = 'BASE TABLE'
ORDER BY table_name;

-- Step 2: Check what Liquibase thinks has been executed
SELECT id, filename, dateexecuted, orderexecuted, exectype
FROM databasechangelog 
ORDER BY orderexecuted;

-- Step 3: Delete changesets for tables that don't exist
-- This will allow Liquibase to recreate them

-- Delete changeset for 'services' table if it doesn't exist
DELETE FROM databasechangelog 
WHERE filename LIKE '%services%' 
  OR id LIKE '%service%';

-- Delete changeset for 'employee_dependant_groups' table if it doesn't exist
DELETE FROM databasechangelog 
WHERE filename LIKE '%employee%group%' 
  OR id LIKE '%employee%group%';

-- Delete changeset for 'failed_external_claim_log' if it exists but Liquibase failed
DELETE FROM databasechangelog 
WHERE filename LIKE '%failed-external-claim-log%' 
  OR id LIKE '%failed-external-claim-log%';

-- Step 4: Verify what's left
SELECT id, filename, dateexecuted, orderexecuted
FROM databasechangelog 
ORDER BY orderexecuted DESC
LIMIT 20;

-- ========================================
-- ALTERNATIVE: Complete Reset (Nuclear Option)
-- ========================================
-- If you want to start completely fresh, uncomment and run these:

-- TRUNCATE TABLE databasechangelog;
-- TRUNCATE TABLE databasechangeloglock;

-- This will make Liquibase recreate ALL tables from scratch
-- WARNING: Only use this if you're okay with Liquibase managing everything

