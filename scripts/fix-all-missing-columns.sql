-- Comprehensive SQL script to fix all missing columns
-- This script adds both is_cbhi column to payers table and attachment columns to medication_dispensing table

-- ========================================
-- 1. Fix is_cbhi column in payers table
-- ========================================

DO $$
BEGIN
    -- Add is_cbhi column to payers table
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'payers' 
          AND column_name = 'is_cbhi' 
          AND table_schema = 'public'
    ) THEN
        ALTER TABLE payers ADD COLUMN is_cbhi BOOLEAN DEFAULT false NOT NULL;
        RAISE NOTICE 'SUCCESS: Added is_cbhi column to payers table';
        
        -- Add index for better query performance
        CREATE INDEX IF NOT EXISTS idx_payers_is_cbhi ON payers(is_cbhi);
        RAISE NOTICE 'SUCCESS: Added index on is_cbhi column';
        
        -- Update payers that contain CBHI-related keywords
        UPDATE payers 
        SET is_cbhi = true 
        WHERE LOWER(payer_name) LIKE '%cbhi%' 
           OR LOWER(payer_name) LIKE '%community%health%insurance%'
           OR LOWER(payer_name) LIKE '%community%based%health%'
           OR LOWER(description) LIKE '%cbhi%'
           OR LOWER(description) LIKE '%community%health%insurance%';
        
        RAISE NOTICE 'SUCCESS: Updated % CBHI payers based on naming conventions', 
                     (SELECT COUNT(*) FROM payers WHERE is_cbhi = true);
    ELSE
        RAISE NOTICE 'INFO: is_cbhi column already exists in payers table';
    END IF;
END $$;

-- ========================================
-- 2. Fix attachment columns in medication_dispensing table
-- ========================================

DO $$
BEGIN
    -- Add attachment_file_name column
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'medication_dispensing' 
          AND column_name = 'attachment_file_name' 
          AND table_schema = 'public'
    ) THEN
        ALTER TABLE medication_dispensing ADD COLUMN attachment_file_name VARCHAR(255);
        RAISE NOTICE 'SUCCESS: Added attachment_file_name column to medication_dispensing table';
    ELSE
        RAISE NOTICE 'INFO: attachment_file_name column already exists';
    END IF;

    -- Add attachment_content_type column
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'medication_dispensing' 
          AND column_name = 'attachment_content_type' 
          AND table_schema = 'public'
    ) THEN
        ALTER TABLE medication_dispensing ADD COLUMN attachment_content_type VARCHAR(100);
        RAISE NOTICE 'SUCCESS: Added attachment_content_type column to medication_dispensing table';
    ELSE
        RAISE NOTICE 'INFO: attachment_content_type column already exists';
    END IF;

    -- Add attachment_data column
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'medication_dispensing' 
          AND column_name = 'attachment_data' 
          AND table_schema = 'public'
    ) THEN
        ALTER TABLE medication_dispensing ADD COLUMN attachment_data BYTEA;
        RAISE NOTICE 'SUCCESS: Added attachment_data column to medication_dispensing table';
    ELSE
        RAISE NOTICE 'INFO: attachment_data column already exists';
    END IF;

    -- Add indexes for better performance
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE tablename = 'medication_dispensing' 
          AND indexname = 'idx_medication_dispensing_attachment_file'
    ) THEN
        CREATE INDEX idx_medication_dispensing_attachment_file ON medication_dispensing(attachment_file_name);
        RAISE NOTICE 'SUCCESS: Added index on attachment_file_name';
    ELSE
        RAISE NOTICE 'INFO: Index on attachment_file_name already exists';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE tablename = 'medication_dispensing' 
          AND indexname = 'idx_medication_dispensing_attachment_type'
    ) THEN
        CREATE INDEX idx_medication_dispensing_attachment_type ON medication_dispensing(attachment_content_type);
        RAISE NOTICE 'SUCCESS: Added index on attachment_content_type';
    ELSE
        RAISE NOTICE 'INFO: Index on attachment_content_type already exists';
    END IF;
END $$;

-- ========================================
-- 3. Verification and Testing
-- ========================================

-- Verify payers table columns
SELECT 
    'PAYERS TABLE VERIFICATION' as check_type,
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns 
WHERE table_name = 'payers' 
  AND column_name = 'is_cbhi'
  AND table_schema = 'public';

-- Verify medication_dispensing table columns
SELECT 
    'MEDICATION_DISPENSING TABLE VERIFICATION' as check_type,
    column_name,
    data_type,
    is_nullable,
    character_maximum_length
FROM information_schema.columns 
WHERE table_name = 'medication_dispensing' 
  AND column_name IN ('attachment_file_name', 'attachment_content_type', 'attachment_data')
  AND table_schema = 'public'
ORDER BY column_name;

-- Test queries that were failing
SELECT 
    'PAYERS TEST QUERY' as test_type,
    COUNT(*) as total_payers,
    COUNT(*) FILTER (WHERE is_cbhi = true) as cbhi_payers,
    COUNT(*) FILTER (WHERE is_cbhi = false) as non_cbhi_payers
FROM payers;

SELECT 
    'MEDICATION_DISPENSING TEST QUERY' as test_type,
    COUNT(*) as total_records,
    COUNT(attachment_file_name) as records_with_filename,
    COUNT(attachment_content_type) as records_with_content_type,
    COUNT(attachment_data) as records_with_data
FROM medication_dispensing;

-- Show sample data
SELECT 
    'SAMPLE PAYERS DATA' as info_type,
    payer_name,
    is_cbhi,
    is_insurance,
    status
FROM payers 
ORDER BY is_cbhi DESC, payer_name
LIMIT 5;

-- Check indexes
SELECT 
    'INDEXES VERIFICATION' as check_type,
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes 
WHERE tablename IN ('payers', 'medication_dispensing')
  AND (indexname LIKE '%cbhi%' OR indexname LIKE '%attachment%')
ORDER BY tablename, indexname;

-- Final summary
DO $$
DECLARE
    payers_cbhi_exists BOOLEAN;
    med_attachment_file_exists BOOLEAN;
    med_attachment_type_exists BOOLEAN;
    med_attachment_data_exists BOOLEAN;
BEGIN
    -- Check if all columns exist
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'payers' AND column_name = 'is_cbhi'
    ) INTO payers_cbhi_exists;
    
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'medication_dispensing' AND column_name = 'attachment_file_name'
    ) INTO med_attachment_file_exists;
    
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'medication_dispensing' AND column_name = 'attachment_content_type'
    ) INTO med_attachment_type_exists;
    
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'medication_dispensing' AND column_name = 'attachment_data'
    ) INTO med_attachment_data_exists;
    
    -- Report final status
    IF payers_cbhi_exists AND med_attachment_file_exists AND med_attachment_type_exists AND med_attachment_data_exists THEN
        RAISE NOTICE '========================================';
        RAISE NOTICE '✅ ALL MISSING COLUMNS FIXED SUCCESSFULLY!';
        RAISE NOTICE '========================================';
        RAISE NOTICE '✅ payers.is_cbhi column: READY';
        RAISE NOTICE '✅ medication_dispensing.attachment_file_name column: READY';
        RAISE NOTICE '✅ medication_dispensing.attachment_content_type column: READY';
        RAISE NOTICE '✅ medication_dispensing.attachment_data column: READY';
        RAISE NOTICE '';
        RAISE NOTICE '🚀 You can now restart your application!';
        RAISE NOTICE '🚀 Both errors should be resolved:';
        RAISE NOTICE '   - "column p1_0.is_cbhi does not exist"';
        RAISE NOTICE '   - "column md1_0.attachment_content_type does not exist"';
        RAISE NOTICE '========================================';
    ELSE
        RAISE WARNING '❌ Some columns are still missing. Please check the output above.';
    END IF;
END $$;
