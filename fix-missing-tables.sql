-- ========================================
-- Fix Missing Tables Issue
-- ========================================
-- Run this SQL in pgAdmin to fix the Liquibase tracking
-- This will allow Liquibase to recreate only the missing tables

-- Step 1: Check which tables are missing
DO $$
BEGIN
    RAISE NOTICE 'Checking for missing tables...';
    
    IF NOT EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'services') THEN
        RAISE NOTICE 'Table "services" is MISSING';
    ELSE
        RAISE NOTICE 'Table "services" exists';
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'servicelists') THEN
        RAISE NOTICE 'Table "servicelists" is MISSING';
    ELSE
        RAISE NOTICE 'Table "servicelists" exists';
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'employee_dependant_groups') THEN
        RAISE NOTICE 'Table "employee_dependant_groups" is MISSING';
    ELSE
        RAISE NOTICE 'Table "employee_dependant_groups" exists';
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'failed_external_claim_log') THEN
        RAISE NOTICE 'Table "failed_external_claim_log" is MISSING';
    ELSE
        RAISE NOTICE 'Table "failed_external_claim_log" exists';
    END IF;
END $$;

-- Step 2: Remove Liquibase tracking for changesets that create missing tables
-- This will force Liquibase to recreate them

-- Remove tracking for services/servicelists table
DELETE FROM databasechangelog 
WHERE id IN (
    'recreate-servicelists-table',
    'create-servicelists-table',
    'create-services-table'
);

-- Remove tracking for employee_dependant_groups table
DELETE FROM databasechangelog 
WHERE id IN (
    'recreate-employee-dependant-groups-table',
    'create-employee-dependant-groups-table'
);

-- Remove tracking for failed_external_claim_log table
DELETE FROM databasechangelog 
WHERE id = 'create-failed-external-claim-log-table';

-- Step 3: Check what changesets are still tracked
SELECT 
    id, 
    filename, 
    dateexecuted, 
    orderexecuted,
    exectype
FROM databasechangelog 
ORDER BY orderexecuted DESC
LIMIT 30;

-- Step 4: Fix foreign key constraint issues
-- Remove invalid foreign key references in insured table
DO $$
BEGIN
    -- Check if there are insured records with invalid group_id
    IF EXISTS (
        SELECT 1 FROM insured 
        WHERE group_id IS NOT NULL 
        AND group_id NOT IN (SELECT id FROM employee_dependant_groups WHERE id IS NOT NULL)
    ) THEN
        RAISE NOTICE 'Found insured records with invalid group_id. Setting to NULL...';
        UPDATE insured SET group_id = NULL 
        WHERE group_id IS NOT NULL 
        AND group_id NOT IN (SELECT id FROM employee_dependant_groups WHERE id IS NOT NULL);
    END IF;
END $$;

-- ========================================
-- ALTERNATIVE: Complete Liquibase Reset
-- ========================================
-- If the above doesn't work, uncomment these lines to completely reset Liquibase
-- WARNING: This will make Liquibase try to recreate ALL tables

-- TRUNCATE TABLE databasechangelog;
-- TRUNCATE TABLE databasechangeloglock;

-- After truncating, Liquibase will run all changesets from scratch
-- Make sure your tables won't conflict with existing ones

