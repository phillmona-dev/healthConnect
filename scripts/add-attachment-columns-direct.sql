-- Direct SQL script to add missing attachment columns to medication_dispensing table
-- Run this script directly in PostgreSQL if Liquibase is having issues

-- Check and add attachment columns
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
        RAISE NOTICE 'Added attachment_file_name column';
    ELSE
        RAISE NOTICE 'attachment_file_name column already exists';
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
        RAISE NOTICE 'Added attachment_content_type column';
    ELSE
        RAISE NOTICE 'attachment_content_type column already exists';
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
        RAISE NOTICE 'Added attachment_data column';
    ELSE
        RAISE NOTICE 'attachment_data column already exists';
    END IF;

    -- Add indexes for better performance
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE tablename = 'medication_dispensing' 
          AND indexname = 'idx_medication_dispensing_attachment_file'
    ) THEN
        CREATE INDEX idx_medication_dispensing_attachment_file ON medication_dispensing(attachment_file_name);
        RAISE NOTICE 'Added index on attachment_file_name';
    ELSE
        RAISE NOTICE 'Index on attachment_file_name already exists';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE tablename = 'medication_dispensing' 
          AND indexname = 'idx_medication_dispensing_attachment_type'
    ) THEN
        CREATE INDEX idx_medication_dispensing_attachment_type ON medication_dispensing(attachment_content_type);
        RAISE NOTICE 'Added index on attachment_content_type';
    ELSE
        RAISE NOTICE 'Index on attachment_content_type already exists';
    END IF;

    RAISE NOTICE 'All attachment columns and indexes processed successfully';
END $$;

-- Verify the columns were added
SELECT 
    'Column Verification' as check_type,
    column_name,
    data_type,
    is_nullable,
    character_maximum_length
FROM information_schema.columns 
WHERE table_name = 'medication_dispensing' 
  AND column_name IN ('attachment_file_name', 'attachment_content_type', 'attachment_data')
  AND table_schema = 'public'
ORDER BY column_name;

-- Check indexes
SELECT 
    'Index Verification' as check_type,
    indexname,
    indexdef
FROM pg_indexes 
WHERE tablename = 'medication_dispensing' 
  AND indexname LIKE '%attachment%'
ORDER BY indexname;

-- Test the columns with a sample query
SELECT 
    'Test Query' as test_type,
    COUNT(*) as total_records,
    COUNT(attachment_file_name) as records_with_filename,
    COUNT(attachment_content_type) as records_with_content_type,
    COUNT(attachment_data) as records_with_data
FROM medication_dispensing;

-- Show table structure for verification
SELECT 
    'Table Structure' as info_type,
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns 
WHERE table_name = 'medication_dispensing' 
  AND table_schema = 'public'
ORDER BY ordinal_position;

RAISE NOTICE 'Script completed successfully. All attachment columns are now available.';
