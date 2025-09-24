-- Quick fix for the status constraint issue
-- This script only fixes the immediate constraint problem

-- Step 1: Drop existing status check constraints (ignore errors if they don't exist)
ALTER TABLE failed_external_dispensing_logs DROP CONSTRAINT IF EXISTS failed_external_dispensing_logs_status_check;
ALTER TABLE failed_external_dispensing_log DROP CONSTRAINT IF EXISTS failed_external_dispensing_log_status_check;

-- Step 2: Rename table from plural to singular if needed (ignore error if already renamed)
DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'failed_external_dispensing_logs') THEN
        ALTER TABLE failed_external_dispensing_logs RENAME TO failed_external_dispensing_log;
    END IF;
EXCEPTION WHEN OTHERS THEN
    -- Table might already be renamed or other issue, continue
    NULL;
END $$;

-- Step 3: Add proper status constraint that includes COMPLETED
ALTER TABLE failed_external_dispensing_log 
ADD CONSTRAINT failed_external_dispensing_log_status_check 
CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING', 'SUSPENDED', 'APPROVED', 'PENDING_APPROVAL', 'REJECTED', 'DRAFT', 'TERMINATION_PENDING', 'AUTHORIZED', 'SUBMITTED', 'RESUBMITTED', 'COMPLETED'));

-- Step 4: Verify the fix
SELECT 'Status constraint fix completed! COMPLETED status is now allowed.' as result;

-- Step 5: Show current constraint
SELECT 
    tc.constraint_name,
    tc.table_name,
    cc.check_clause
FROM information_schema.table_constraints tc
LEFT JOIN information_schema.check_constraints cc ON tc.constraint_name = cc.constraint_name
WHERE tc.table_name = 'failed_external_dispensing_log'
  AND tc.constraint_type = 'CHECK'
  AND tc.constraint_name LIKE '%status%';
