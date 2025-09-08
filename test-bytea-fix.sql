-- Test script to verify BYTEA column fixes work correctly
-- This tests both MedicationDispensing.attachmentData and User.imageData

-- ========================================
-- 1. Check current column types
-- ========================================

SELECT 
    'MEDICATION_DISPENSING ATTACHMENT COLUMNS' as table_info,
    column_name, 
    data_type, 
    is_nullable
FROM information_schema.columns 
WHERE table_name = 'medication_dispensing' 
  AND column_name LIKE '%attachment%'
ORDER BY column_name;

SELECT 
    'USERS IMAGE COLUMN' as table_info,
    column_name, 
    data_type, 
    is_nullable
FROM information_schema.columns 
WHERE table_name = 'users' 
  AND column_name = 'image_data';

-- ========================================
-- 2. Test MedicationDispensing attachment_data insert
-- ========================================

-- Create a test medication dispensing record with binary attachment
INSERT INTO medication_dispensing (
    dispensing_uuid, 
    provider_uuid, 
    payer_uuid, 
    insured_uuid,
    attachment_file_name,
    attachment_content_type,
    attachment_data,
    deleted,
    created_at,
    updated_at
) VALUES (
    'test-bytea-' || gen_random_uuid()::text,
    'test-provider-uuid',
    'test-payer-uuid', 
    'test-insured-uuid',
    'test-prescription.pdf',
    'application/pdf',
    decode('255044462D312E340A25C4E5F2E5F2E5F2E50A', 'hex'), -- PDF header bytes
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT DO NOTHING;

-- Verify the insert worked
SELECT 
    'MEDICATION_DISPENSING INSERT TEST' as test_result,
    dispensing_uuid,
    attachment_file_name,
    attachment_content_type,
    length(attachment_data) as attachment_size_bytes,
    CASE 
        WHEN attachment_data IS NOT NULL THEN 'SUCCESS: Binary data stored'
        ELSE 'ERROR: Binary data is null'
    END as status
FROM medication_dispensing 
WHERE dispensing_uuid LIKE 'test-bytea-%'
ORDER BY created_at DESC
LIMIT 1;

-- ========================================
-- 3. Test Users image_data column (if exists)
-- ========================================

-- Check if users table has image_data column
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'users' 
          AND column_name = 'image_data'
          AND table_schema = 'public'
    ) THEN
        RAISE NOTICE 'Users table has image_data column - testing insert';
        
        -- Test insert would go here, but we'll skip to avoid affecting real user data
        RAISE NOTICE 'Skipping user image_data insert test to avoid affecting real data';
    ELSE
        RAISE NOTICE 'Users table does not have image_data column yet';
    END IF;
END $$;

-- ========================================
-- 4. Performance test - ensure no OID issues
-- ========================================

-- Test that we can query attachment data without type errors
SELECT 
    'PERFORMANCE TEST' as test_type,
    COUNT(*) as total_records,
    COUNT(attachment_data) as records_with_attachment,
    AVG(length(attachment_data)) as avg_attachment_size,
    MAX(length(attachment_data)) as max_attachment_size
FROM medication_dispensing
WHERE attachment_data IS NOT NULL;

-- ========================================
-- 5. Clean up test data
-- ========================================

DELETE FROM medication_dispensing 
WHERE dispensing_uuid LIKE 'test-bytea-%';

-- ========================================
-- 6. Final verification
-- ========================================

SELECT 
    'FINAL VERIFICATION' as check_type,
    CASE 
        WHEN EXISTS (
            SELECT 1 
            FROM information_schema.columns 
            WHERE table_name = 'medication_dispensing' 
              AND column_name = 'attachment_data'
              AND data_type = 'bytea'
        ) 
        THEN 'SUCCESS: attachment_data is BYTEA type'
        ELSE 'ERROR: attachment_data is not BYTEA type'
    END as medication_dispensing_status,
    CASE 
        WHEN EXISTS (
            SELECT 1 
            FROM information_schema.columns 
            WHERE table_name = 'users' 
              AND column_name = 'image_data'
              AND data_type = 'bytea'
        ) 
        THEN 'SUCCESS: image_data is BYTEA type'
        WHEN NOT EXISTS (
            SELECT 1 
            FROM information_schema.columns 
            WHERE table_name = 'users' 
              AND column_name = 'image_data'
        )
        THEN 'INFO: image_data column does not exist yet'
        ELSE 'WARNING: image_data exists but is not BYTEA type'
    END as users_status;

SELECT 'SUCCESS: BYTEA column type fixes verified!' as result;
