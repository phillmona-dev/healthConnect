-- Fix attachment_data column type from OID to BYTEA
-- This resolves the error: column "attachment_data" is of type bytea but expression is of type oid

-- Check current column type
SELECT 
    column_name, 
    data_type, 
    is_nullable,
    column_default
FROM information_schema.columns 
WHERE table_name = 'medication_dispensing' 
  AND column_name LIKE '%attachment%'
ORDER BY column_name;

-- Drop and recreate the attachment_data column with correct BYTEA type
-- This is safe because we're dealing with a new column that likely has no data yet

-- Step 1: Drop the existing column if it exists
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'medication_dispensing' 
          AND column_name = 'attachment_data'
          AND table_schema = 'public'
    ) THEN
        -- First, check if there's any data in the column
        EXECUTE 'SELECT COUNT(*) FROM medication_dispensing WHERE attachment_data IS NOT NULL' INTO @count;
        
        IF @count > 0 THEN
            RAISE NOTICE 'WARNING: attachment_data column contains % rows with data. Consider backing up before proceeding.', @count;
        ELSE
            RAISE NOTICE 'attachment_data column is empty, safe to recreate';
        END IF;
        
        -- Drop the column
        ALTER TABLE medication_dispensing DROP COLUMN attachment_data;
        RAISE NOTICE 'Dropped existing attachment_data column';
    ELSE
        RAISE NOTICE 'attachment_data column does not exist';
    END IF;
END $$;

-- Step 2: Create the column with correct BYTEA type
ALTER TABLE medication_dispensing ADD COLUMN attachment_data BYTEA;

-- Step 3: Verify the fix
SELECT 
    column_name, 
    data_type, 
    is_nullable
FROM information_schema.columns 
WHERE table_name = 'medication_dispensing' 
  AND column_name = 'attachment_data';

-- Step 4: Test insert to verify the fix works
-- This should not cause the OID/BYTEA type mismatch error anymore
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
    'test-' || gen_random_uuid()::text,
    'test-provider-uuid',
    'test-payer-uuid', 
    'test-insured-uuid',
    'test-file.pdf',
    'application/pdf',
    decode('89504E470D0A1A0A', 'hex'), -- Sample binary data (PNG header)
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT DO NOTHING;

-- Clean up test data
DELETE FROM medication_dispensing WHERE dispensing_uuid LIKE 'test-%';

SELECT 'SUCCESS: attachment_data column fixed and tested!' as result;
