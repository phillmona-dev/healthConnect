-- Verification script for missing claim tables
-- Run this after executing the Liquibase update

-- Check if all missing tables exist
SELECT 
    'Table Existence Check' as check_type,
    CASE 
        WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'batch_records' AND table_schema = 'public') 
        THEN '✓ batch_records' 
        ELSE '✗ batch_records MISSING' 
    END as batch_records,
    CASE 
        WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'batch_logs' AND table_schema = 'public') 
        THEN '✓ batch_logs' 
        ELSE '✗ batch_logs MISSING' 
    END as batch_logs,
    CASE 
        WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'claim_attachments' AND table_schema = 'public') 
        THEN '✓ claim_attachments' 
        ELSE '✗ claim_attachments MISSING' 
    END as claim_attachments,
    CASE 
        WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'claim_comments' AND table_schema = 'public') 
        THEN '✓ claim_comments' 
        ELSE '✗ claim_comments MISSING' 
    END as claim_comments,
    CASE 
        WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'claim_items' AND table_schema = 'public') 
        THEN '✓ claim_items' 
        ELSE '✗ claim_items MISSING' 
    END as claim_items,
    CASE 
        WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'claim_logs' AND table_schema = 'public') 
        THEN '✓ claim_logs' 
        ELSE '✗ claim_logs MISSING' 
    END as claim_logs,
    CASE 
        WHEN EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'claim_payments' AND table_schema = 'public') 
        THEN '✓ claim_payments' 
        ELSE '✗ claim_payments MISSING' 
    END as claim_payments;

-- Check table structures
SELECT 
    table_name,
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns 
WHERE table_schema = 'public' 
    AND table_name IN ('batch_records', 'batch_logs', 'claim_attachments', 'claim_comments', 'claim_items', 'claim_logs', 'claim_payments')
ORDER BY table_name, ordinal_position;

-- Check foreign key constraints
SELECT 
    tc.constraint_name,
    tc.table_name,
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
    AND tc.table_schema = kcu.table_schema
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
    AND ccu.table_schema = tc.table_schema
WHERE tc.constraint_type = 'FOREIGN KEY'
    AND tc.table_schema = 'public'
    AND tc.table_name IN ('batch_logs', 'claim_attachments', 'claim_comments', 'claim_items', 'claim_logs', 'claim_payments')
ORDER BY tc.table_name, tc.constraint_name;

-- Check Liquibase changelog entries
SELECT 
    id,
    author,
    filename,
    dateexecuted,
    orderexecuted,
    exectype
FROM databasechangelog 
WHERE id IN ('create-missing-claim-tables', 'add-missing-foreign-keys')
ORDER BY orderexecuted DESC;

-- Summary report
SELECT 
    'SUMMARY REPORT' as report_type,
    (SELECT COUNT(*) FROM information_schema.tables 
     WHERE table_schema = 'public' 
     AND table_name IN ('batch_records', 'batch_logs', 'claim_attachments', 'claim_comments', 'claim_items', 'claim_logs', 'claim_payments')
    ) as tables_created,
    '7' as tables_expected,
    CASE 
        WHEN (SELECT COUNT(*) FROM information_schema.tables 
              WHERE table_schema = 'public' 
              AND table_name IN ('batch_records', 'batch_logs', 'claim_attachments', 'claim_comments', 'claim_items', 'claim_logs', 'claim_payments')
             ) = 7 
        THEN 'SUCCESS: All tables created' 
        ELSE 'ERROR: Some tables missing' 
    END as status;

-- Test table accessibility (insert/select permissions)
DO $$
DECLARE
    table_name TEXT;
    table_list TEXT[] := ARRAY['batch_records', 'batch_logs', 'claim_attachments', 'claim_comments', 'claim_items', 'claim_logs', 'claim_payments'];
BEGIN
    RAISE NOTICE 'Testing table accessibility...';
    
    FOREACH table_name IN ARRAY table_list
    LOOP
        BEGIN
            EXECUTE format('SELECT COUNT(*) FROM %I', table_name);
            RAISE NOTICE 'Table %: ACCESSIBLE', table_name;
        EXCEPTION WHEN OTHERS THEN
            RAISE WARNING 'Table %: NOT ACCESSIBLE - %', table_name, SQLERRM;
        END;
    END LOOP;
END $$;
