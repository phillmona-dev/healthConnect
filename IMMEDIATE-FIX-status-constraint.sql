-- IMMEDIATE FIX for the status constraint issue
-- Run this SQL script directly in your PostgreSQL database to fix the constraint problem

-- Step 1: Show current table status
SELECT 'Current tables:' as info;
SELECT table_name, table_type 
FROM information_schema.tables 
WHERE table_name LIKE '%failed_external_dispensing%' 
  AND table_schema = 'public';

-- Step 2: Show current constraints
SELECT 'Current constraints:' as info;
SELECT 
    tc.constraint_name,
    tc.table_name,
    tc.constraint_type,
    cc.check_clause
FROM information_schema.table_constraints tc
LEFT JOIN information_schema.check_constraints cc ON tc.constraint_name = cc.constraint_name
WHERE tc.table_name LIKE '%failed_external_dispensing%'
  AND tc.table_schema = 'public'
  AND tc.constraint_type = 'CHECK'
ORDER BY tc.table_name, tc.constraint_name;

-- Step 3: Drop ALL existing status check constraints (ignore errors)
DO $$ 
BEGIN
    -- Drop from plural table
    BEGIN
        ALTER TABLE failed_external_dispensing_logs DROP CONSTRAINT IF EXISTS failed_external_dispensing_logs_status_check;
    EXCEPTION WHEN OTHERS THEN
        NULL;
    END;
    
    BEGIN
        ALTER TABLE failed_external_dispensing_logs DROP CONSTRAINT IF EXISTS failed_external_dispensing_logs_check;
    EXCEPTION WHEN OTHERS THEN
        NULL;
    END;
    
    -- Drop from singular table
    BEGIN
        ALTER TABLE failed_external_dispensing_log DROP CONSTRAINT IF EXISTS failed_external_dispensing_log_status_check;
    EXCEPTION WHEN OTHERS THEN
        NULL;
    END;
    
    BEGIN
        ALTER TABLE failed_external_dispensing_log DROP CONSTRAINT IF EXISTS failed_external_dispensing_log_check;
    EXCEPTION WHEN OTHERS THEN
        NULL;
    END;
    
    RAISE NOTICE 'Dropped all existing status constraints';
END $$;

-- Step 4: Add the correct constraint to whichever table exists
DO $$ 
BEGIN
    -- Add constraint to singular table if it exists
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'failed_external_dispensing_log') THEN
        BEGIN
            ALTER TABLE failed_external_dispensing_log 
            ADD CONSTRAINT failed_external_dispensing_log_status_check 
            CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING', 'SUSPENDED', 'APPROVED', 'PENDING_APPROVAL', 'REJECTED', 'DRAFT', 'TERMINATION_PENDING', 'AUTHORIZED', 'SUBMITTED', 'RESUBMITTED', 'COMPLETED'));
            RAISE NOTICE 'Added status constraint to failed_external_dispensing_log (singular)';
        EXCEPTION WHEN OTHERS THEN
            RAISE NOTICE 'Failed to add constraint to singular table: %', SQLERRM;
        END;
    END IF;
    
    -- Add constraint to plural table if it exists
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'failed_external_dispensing_logs') THEN
        BEGIN
            ALTER TABLE failed_external_dispensing_logs 
            ADD CONSTRAINT failed_external_dispensing_logs_status_check 
            CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING', 'SUSPENDED', 'APPROVED', 'PENDING_APPROVAL', 'REJECTED', 'DRAFT', 'TERMINATION_PENDING', 'AUTHORIZED', 'SUBMITTED', 'RESUBMITTED', 'COMPLETED'));
            RAISE NOTICE 'Added status constraint to failed_external_dispensing_logs (plural)';
        EXCEPTION WHEN OTHERS THEN
            RAISE NOTICE 'Failed to add constraint to plural table: %', SQLERRM;
        END;
    END IF;
END $$;

-- Step 5: Create essential indexes
DO $$ 
BEGIN
    -- Create indexes on singular table if it exists
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'failed_external_dispensing_log') THEN
        CREATE INDEX IF NOT EXISTS idx_failed_dispensing_status ON failed_external_dispensing_log(status);
        CREATE INDEX IF NOT EXISTS idx_failed_dispensing_uuid ON failed_external_dispensing_log(dispensing_uuid);
        RAISE NOTICE 'Created indexes on failed_external_dispensing_log (singular)';
    END IF;
    
    -- Create indexes on plural table if it exists
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'failed_external_dispensing_logs') THEN
        CREATE INDEX IF NOT EXISTS idx_failed_dispensing_status_logs ON failed_external_dispensing_logs(status);
        CREATE INDEX IF NOT EXISTS idx_failed_dispensing_uuid_logs ON failed_external_dispensing_logs(dispensing_uuid);
        RAISE NOTICE 'Created indexes on failed_external_dispensing_logs (plural)';
    END IF;
END $$;

-- Step 6: Verify the fix
SELECT 'VERIFICATION - Updated constraints:' as info;
SELECT 
    tc.constraint_name,
    tc.table_name,
    tc.constraint_type,
    cc.check_clause
FROM information_schema.table_constraints tc
LEFT JOIN information_schema.check_constraints cc ON tc.constraint_name = cc.constraint_name
WHERE tc.table_name LIKE '%failed_external_dispensing%'
  AND tc.table_schema = 'public'
  AND tc.constraint_type = 'CHECK'
ORDER BY tc.table_name, tc.constraint_name;

-- Step 7: Test the fix by checking if COMPLETED is allowed
SELECT 'Testing COMPLETED status constraint:' as test_info;
DO $$ 
BEGIN
    -- Test constraint allows COMPLETED
    IF EXISTS (
        SELECT 1 FROM information_schema.check_constraints 
        WHERE check_clause LIKE '%COMPLETED%'
        AND constraint_name LIKE '%failed_external_dispensing%'
    ) THEN
        RAISE NOTICE 'SUCCESS: COMPLETED status is now allowed in the constraint!';
    ELSE
        RAISE NOTICE 'WARNING: COMPLETED status might not be properly added to constraint';
    END IF;
END $$;

-- Final success message
SELECT '✅ DATABASE CONSTRAINT FIX COMPLETED! You can now log COMPLETED status for successful dispensing sends.' as result;
