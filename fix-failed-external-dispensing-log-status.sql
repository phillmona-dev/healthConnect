-- Fix failed_external_dispensing_log table status constraint to allow COMPLETED status
-- This script addresses the PostgreSQL constraint violation error

-- Step 1: Check current table structure
SELECT 
    'Current table structure:' as info,
    table_name,
    column_name,
    data_type,
    is_nullable
FROM information_schema.columns 
WHERE table_name IN ('failed_external_dispensing_log', 'failed_external_dispensing_logs')
  AND table_schema = 'public'
ORDER BY table_name, ordinal_position;

-- Step 2: Check existing constraints
SELECT 
    'Current constraints:' as info,
    tc.constraint_name,
    tc.table_name,
    tc.constraint_type,
    cc.check_clause
FROM information_schema.table_constraints tc
LEFT JOIN information_schema.check_constraints cc ON tc.constraint_name = cc.constraint_name
WHERE tc.table_name IN ('failed_external_dispensing_log', 'failed_external_dispensing_logs')
  AND tc.table_schema = 'public'
ORDER BY tc.table_name, tc.constraint_name;

-- Step 3: Drop existing status check constraints
DO $$
BEGIN
    -- Drop constraint from failed_external_dispensing_logs (plural) if it exists
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE table_name = 'failed_external_dispensing_logs' 
        AND constraint_type = 'CHECK' 
        AND constraint_name LIKE '%status%'
    ) THEN
        EXECUTE 'ALTER TABLE failed_external_dispensing_logs DROP CONSTRAINT ' || 
            (SELECT constraint_name FROM information_schema.table_constraints 
             WHERE table_name = 'failed_external_dispensing_logs' 
             AND constraint_type = 'CHECK' 
             AND constraint_name LIKE '%status%' 
             LIMIT 1);
        RAISE NOTICE 'Dropped status constraint from failed_external_dispensing_logs';
    END IF;
    
    -- Drop constraint from failed_external_dispensing_log (singular) if it exists
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE table_name = 'failed_external_dispensing_log' 
        AND constraint_type = 'CHECK' 
        AND constraint_name LIKE '%status%'
    ) THEN
        EXECUTE 'ALTER TABLE failed_external_dispensing_log DROP CONSTRAINT ' || 
            (SELECT constraint_name FROM information_schema.table_constraints 
             WHERE table_name = 'failed_external_dispensing_log' 
             AND constraint_type = 'CHECK' 
             AND constraint_name LIKE '%status%' 
             LIMIT 1);
        RAISE NOTICE 'Dropped status constraint from failed_external_dispensing_log';
    END IF;
END $$;

-- Step 4: Rename table from plural to singular if needed
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'failed_external_dispensing_logs') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'failed_external_dispensing_log') THEN
            ALTER TABLE failed_external_dispensing_logs RENAME TO failed_external_dispensing_log;
            RAISE NOTICE 'Renamed table from failed_external_dispensing_logs to failed_external_dispensing_log';
        ELSE
            RAISE NOTICE 'Both tables exist - manual intervention needed';
        END IF;
    ELSE
        RAISE NOTICE 'Table failed_external_dispensing_logs does not exist';
    END IF;
END $$;

-- Step 5: Ensure the table has all required columns for our entity
DO $$
BEGIN
    -- Add missing columns if they don't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'failed_external_dispensing_log' AND column_name = 'dispensing_item_uuid') THEN
        ALTER TABLE failed_external_dispensing_log ADD COLUMN dispensing_item_uuid VARCHAR(255);
        RAISE NOTICE 'Added dispensing_item_uuid column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'failed_external_dispensing_log' AND column_name = 'service_id') THEN
        ALTER TABLE failed_external_dispensing_log ADD COLUMN service_id VARCHAR(255);
        RAISE NOTICE 'Added service_id column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'failed_external_dispensing_log' AND column_name = 'insured_uuid') THEN
        ALTER TABLE failed_external_dispensing_log ADD COLUMN insured_uuid VARCHAR(255);
        RAISE NOTICE 'Added insured_uuid column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'failed_external_dispensing_log' AND column_name = 'package_uuid') THEN
        ALTER TABLE failed_external_dispensing_log ADD COLUMN package_uuid VARCHAR(255);
        RAISE NOTICE 'Added package_uuid column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'failed_external_dispensing_log' AND column_name = 'quantity') THEN
        ALTER TABLE failed_external_dispensing_log ADD COLUMN quantity INTEGER;
        RAISE NOTICE 'Added quantity column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'failed_external_dispensing_log' AND column_name = 'total_price') THEN
        ALTER TABLE failed_external_dispensing_log ADD COLUMN total_price DOUBLE PRECISION;
        RAISE NOTICE 'Added total_price column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'failed_external_dispensing_log' AND column_name = 'provided_date') THEN
        ALTER TABLE failed_external_dispensing_log ADD COLUMN provided_date VARCHAR(255);
        RAISE NOTICE 'Added provided_date column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'failed_external_dispensing_log' AND column_name = 'provider_uuid') THEN
        ALTER TABLE failed_external_dispensing_log ADD COLUMN provider_uuid VARCHAR(255);
        RAISE NOTICE 'Added provider_uuid column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'failed_external_dispensing_log' AND column_name = 'external_api_url') THEN
        ALTER TABLE failed_external_dispensing_log ADD COLUMN external_api_url TEXT;
        RAISE NOTICE 'Added external_api_url column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'failed_external_dispensing_log' AND column_name = 'last_response') THEN
        ALTER TABLE failed_external_dispensing_log ADD COLUMN last_response TEXT;
        RAISE NOTICE 'Added last_response column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'failed_external_dispensing_log' AND column_name = 'max_retries') THEN
        ALTER TABLE failed_external_dispensing_log ADD COLUMN max_retries INTEGER DEFAULT 5;
        RAISE NOTICE 'Added max_retries column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'failed_external_dispensing_log' AND column_name = 'next_retry_at') THEN
        ALTER TABLE failed_external_dispensing_log ADD COLUMN next_retry_at TIMESTAMP;
        RAISE NOTICE 'Added next_retry_at column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'failed_external_dispensing_log' AND column_name = 'first_failed_at') THEN
        ALTER TABLE failed_external_dispensing_log ADD COLUMN first_failed_at TIMESTAMP;
        RAISE NOTICE 'Added first_failed_at column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'failed_external_dispensing_log' AND column_name = 'last_attempt_at') THEN
        ALTER TABLE failed_external_dispensing_log ADD COLUMN last_attempt_at TIMESTAMP;
        RAISE NOTICE 'Added last_attempt_at column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'failed_external_dispensing_log' AND column_name = 'succeeded_at') THEN
        ALTER TABLE failed_external_dispensing_log ADD COLUMN succeeded_at TIMESTAMP;
        RAISE NOTICE 'Added succeeded_at column';
    END IF;
END $$;

-- Step 6: Add proper status constraint that includes COMPLETED
ALTER TABLE failed_external_dispensing_log 
ADD CONSTRAINT failed_external_dispensing_log_status_check 
CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING', 'SUSPENDED', 'APPROVED', 'PENDING_APPROVAL', 'REJECTED', 'DRAFT', 'TERMINATION_PENDING', 'AUTHORIZED', 'SUBMITTED', 'RESUBMITTED', 'COMPLETED'));

-- Step 7: Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_failed_dispensing_status ON failed_external_dispensing_log(status);
CREATE INDEX IF NOT EXISTS idx_failed_dispensing_retry_count ON failed_external_dispensing_log(retry_count);
CREATE INDEX IF NOT EXISTS idx_failed_dispensing_next_retry ON failed_external_dispensing_log(next_retry_at);
CREATE INDEX IF NOT EXISTS idx_failed_dispensing_item ON failed_external_dispensing_log(dispensing_item_uuid);
CREATE INDEX IF NOT EXISTS idx_failed_dispensing_uuid ON failed_external_dispensing_log(dispensing_uuid);

-- Step 8: Verify the fix
SELECT 
    'Verification - Updated table structure:' as info,
    table_name,
    column_name,
    data_type,
    is_nullable
FROM information_schema.columns 
WHERE table_name = 'failed_external_dispensing_log'
  AND table_schema = 'public'
ORDER BY ordinal_position;

SELECT 
    'Verification - Updated constraints:' as info,
    tc.constraint_name,
    tc.table_name,
    tc.constraint_type,
    cc.check_clause
FROM information_schema.table_constraints tc
LEFT JOIN information_schema.check_constraints cc ON tc.constraint_name = cc.constraint_name
WHERE tc.table_name = 'failed_external_dispensing_log'
  AND tc.table_schema = 'public'
  AND tc.constraint_type = 'CHECK'
ORDER BY tc.constraint_name;

-- Success message
SELECT 'Database constraint fix completed successfully! COMPLETED status is now allowed.' as result;
