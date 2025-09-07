-- Verification script for is_cbhi column addition
-- Run this after executing the Liquibase update

-- Check if is_cbhi column exists
SELECT 
    'Column Existence Check' as check_type,
    CASE 
        WHEN EXISTS (
            SELECT 1 
            FROM information_schema.columns 
            WHERE table_name = 'payers' 
              AND column_name = 'is_cbhi' 
              AND table_schema = 'public'
        ) 
        THEN '✓ is_cbhi column exists' 
        ELSE '✗ is_cbhi column MISSING' 
    END as column_status;

-- Check column details
SELECT 
    column_name,
    data_type,
    is_nullable,
    column_default,
    character_maximum_length
FROM information_schema.columns 
WHERE table_schema = 'public' 
    AND table_name = 'payers' 
    AND column_name = 'is_cbhi';

-- Check indexes on is_cbhi column
SELECT 
    indexname,
    indexdef
FROM pg_indexes 
WHERE tablename = 'payers' 
    AND indexdef LIKE '%is_cbhi%'
ORDER BY indexname;

-- Check current data distribution
SELECT 
    'Data Distribution' as check_type,
    is_cbhi,
    COUNT(*) as payer_count,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER (), 2) as percentage
FROM payers 
GROUP BY is_cbhi
ORDER BY is_cbhi;

-- Show sample payers with CBHI status
SELECT 
    payer_name,
    is_cbhi,
    is_insurance,
    status,
    category
FROM payers 
ORDER BY is_cbhi DESC, payer_name
LIMIT 10;

-- Check for any CBHI payers that might have been auto-detected
SELECT 
    'CBHI Auto-Detection Results' as check_type,
    COUNT(*) as cbhi_payers_found
FROM payers 
WHERE is_cbhi = true;

-- Show CBHI payers if any were found
SELECT 
    payer_name,
    description,
    category,
    is_insurance,
    status
FROM payers 
WHERE is_cbhi = true
ORDER BY payer_name;

-- Verify the column can be queried without errors
DO $$
BEGIN
    PERFORM COUNT(*) FROM payers WHERE is_cbhi = true;
    RAISE NOTICE 'SUCCESS: is_cbhi column is queryable';
EXCEPTION WHEN OTHERS THEN
    RAISE WARNING 'ERROR: is_cbhi column query failed - %', SQLERRM;
END $$;

-- Check Liquibase changelog entries
SELECT 
    id,
    author,
    filename,
    dateexecuted,
    orderexecuted,
    exectype,
    md5sum
FROM databasechangelog 
WHERE id LIKE '%cbhi%' OR filename LIKE '%cbhi%'
ORDER BY orderexecuted DESC;

-- Final summary
SELECT 
    'SUMMARY REPORT' as report_type,
    CASE 
        WHEN EXISTS (
            SELECT 1 
            FROM information_schema.columns 
            WHERE table_name = 'payers' 
              AND column_name = 'is_cbhi' 
              AND table_schema = 'public'
        ) 
        THEN 'SUCCESS: is_cbhi column added successfully' 
        ELSE 'ERROR: is_cbhi column not found' 
    END as status,
    (SELECT COUNT(*) FROM payers) as total_payers,
    (SELECT COUNT(*) FROM payers WHERE is_cbhi = true) as cbhi_payers,
    (SELECT COUNT(*) FROM payers WHERE is_cbhi = false) as non_cbhi_payers;

-- Test a sample query that was failing before
SELECT 
    'Test Query' as test_type,
    COUNT(*) as result_count
FROM payers p1_0 
WHERE p1_0.is_cbhi = false
LIMIT 5;
