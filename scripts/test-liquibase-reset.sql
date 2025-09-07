-- Test script to validate Liquibase non-core tables reset
-- Run this BEFORE and AFTER the Liquibase update to verify the process

-- =====================================================
-- BEFORE LIQUIBASE UPDATE - Check current state
-- =====================================================

-- Check which tables exist
SELECT 
    schemaname,
    tablename,
    CASE 
        WHEN tablename IN ('insured', 'payers', 'role', 'privilege', 'role_privilege', 'app_user', 'providers') 
        THEN 'CORE' 
        ELSE 'NON-CORE' 
    END as table_type
FROM pg_tables 
WHERE schemaname = 'public' 
ORDER BY table_type, tablename;

-- Check sequences
SELECT 
    schemaname,
    sequencename,
    CASE 
        WHEN sequencename LIKE '%insured%' OR sequencename LIKE '%payer%' OR 
             sequencename LIKE '%role%' OR sequencename LIKE '%privilege%' OR 
             sequencename LIKE '%user%' OR sequencename LIKE '%provider%'
        THEN 'CORE' 
        ELSE 'NON-CORE' 
    END as sequence_type
FROM pg_sequences 
WHERE schemaname = 'public'
ORDER BY sequence_type, sequencename;

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
ORDER BY tc.table_name, tc.constraint_name;

-- Check indexes
SELECT 
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes 
WHERE schemaname = 'public'
    AND indexname NOT LIKE '%pkey'
ORDER BY tablename, indexname;

-- Count records in core tables (should be preserved)
SELECT 'insured' as table_name, COUNT(*) as record_count FROM insured
UNION ALL
SELECT 'payers' as table_name, COUNT(*) as record_count FROM payers
UNION ALL
SELECT 'role' as table_name, COUNT(*) as record_count FROM role
UNION ALL
SELECT 'privilege' as table_name, COUNT(*) as record_count FROM privilege
UNION ALL
SELECT 'role_privilege' as table_name, COUNT(*) as record_count FROM role_privilege
UNION ALL
SELECT 'app_user' as table_name, COUNT(*) as record_count FROM app_user
UNION ALL
SELECT 'providers' as table_name, COUNT(*) as record_count FROM providers;

-- =====================================================
-- AFTER LIQUIBASE UPDATE - Verify results
-- =====================================================

-- Verify core tables still exist and have data
DO $$
DECLARE
    core_tables TEXT[] := ARRAY['insured', 'payers', 'role', 'privilege', 'role_privilege', 'app_user', 'providers'];
    table_name TEXT;
    table_count INTEGER;
BEGIN
    RAISE NOTICE 'Checking core tables...';
    
    FOREACH table_name IN ARRAY core_tables
    LOOP
        EXECUTE format('SELECT COUNT(*) FROM %I', table_name) INTO table_count;
        RAISE NOTICE 'Table %: % records', table_name, table_count;
        
        IF table_count = 0 THEN
            RAISE WARNING 'Core table % is empty!', table_name;
        END IF;
    END LOOP;
END $$;

-- Verify non-core tables exist but are empty
DO $$
DECLARE
    non_core_tables TEXT[] := ARRAY[
        'package_categories', 'drugs', 'servicelists', 'contract_headers', 
        'contract_details', 'dependants', 'medication_dispensing', 
        'medication_dispensing_items', 'claims', 'employee_dependant_groups'
    ];
    table_name TEXT;
    table_count INTEGER;
    table_exists BOOLEAN;
BEGIN
    RAISE NOTICE 'Checking non-core tables...';
    
    FOREACH table_name IN ARRAY non_core_tables
    LOOP
        -- Check if table exists
        SELECT EXISTS (
            SELECT FROM information_schema.tables 
            WHERE table_schema = 'public' 
            AND table_name = table_name
        ) INTO table_exists;
        
        IF table_exists THEN
            EXECUTE format('SELECT COUNT(*) FROM %I', table_name) INTO table_count;
            RAISE NOTICE 'Table %: EXISTS with % records', table_name, table_count;
        ELSE
            RAISE WARNING 'Table % does not exist!', table_name;
        END IF;
    END LOOP;
END $$;

-- Check that foreign key relationships are properly restored
SELECT 
    'Foreign key constraints restored' as status,
    COUNT(*) as constraint_count
FROM information_schema.table_constraints 
WHERE constraint_type = 'FOREIGN KEY' 
    AND table_schema = 'public';

-- Check that sequences are properly restored
SELECT 
    'Sequences restored' as status,
    COUNT(*) as sequence_count
FROM pg_sequences 
WHERE schemaname = 'public';

-- Check that indexes are properly restored
SELECT 
    'Indexes restored' as status,
    COUNT(*) as index_count
FROM pg_indexes 
WHERE schemaname = 'public'
    AND indexname NOT LIKE '%pkey';

-- Final validation summary
SELECT 
    CASE 
        WHEN (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'insured') = 1
        AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'package_categories') = 1
        AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'servicelists') = 1
        THEN 'SUCCESS: All tables exist'
        ELSE 'FAILURE: Some tables missing'
    END as validation_result;

-- Show Liquibase changelog status
SELECT 
    id,
    author,
    filename,
    dateexecuted,
    orderexecuted,
    exectype,
    md5sum
FROM databasechangelog 
WHERE filename LIKE '%drop-non-core%' OR filename LIKE '%recreate-non-core%'
ORDER BY orderexecuted DESC;
