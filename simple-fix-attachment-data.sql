-- Simple fix for attachment_data column type
-- This ensures the column is BYTEA type to resolve OID vs BYTEA mismatch

-- Drop and recreate the attachment_data column with correct type
ALTER TABLE medication_dispensing DROP COLUMN IF EXISTS attachment_data;
ALTER TABLE medication_dispensing ADD COLUMN attachment_data BYTEA;

-- Ensure other attachment columns exist
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS attachment_file_name VARCHAR(255);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS attachment_content_type VARCHAR(100);

-- Verify the fix
SELECT 
    column_name, 
    data_type, 
    is_nullable
FROM information_schema.columns 
WHERE table_name = 'medication_dispensing' 
  AND column_name LIKE '%attachment%'
ORDER BY column_name;

SELECT 'SUCCESS: attachment_data column is now BYTEA type!' as result;
