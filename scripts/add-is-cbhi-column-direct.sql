-- Direct SQL script to add is_cbhi column to payers table
-- Run this script directly in PostgreSQL if Liquibase is having issues

-- Check if column already exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'payers' 
          AND column_name = 'is_cbhi' 
          AND table_schema = 'public'
    ) THEN
        -- Add the is_cbhi column
        ALTER TABLE payers ADD COLUMN is_cbhi BOOLEAN DEFAULT false;
        
        -- Update existing records to set default value
        UPDATE payers SET is_cbhi = false WHERE is_cbhi IS NULL;
        
        -- Make the column not nullable
        ALTER TABLE payers ALTER COLUMN is_cbhi SET NOT NULL;
        
        -- Add index for better query performance
        CREATE INDEX idx_payers_is_cbhi ON payers(is_cbhi);
        
        -- Add composite index for common queries
        CREATE INDEX idx_payers_cbhi_insurance_status ON payers(is_cbhi, is_insurance, status);
        
        -- Update payers that contain CBHI-related keywords
        UPDATE payers 
        SET is_cbhi = true 
        WHERE LOWER(payer_name) LIKE '%cbhi%' 
           OR LOWER(payer_name) LIKE '%community%health%insurance%'
           OR LOWER(payer_name) LIKE '%community%based%health%'
           OR LOWER(description) LIKE '%cbhi%'
           OR LOWER(description) LIKE '%community%health%insurance%';
        
        RAISE NOTICE 'SUCCESS: is_cbhi column added to payers table';
        RAISE NOTICE 'Updated % payers with CBHI status based on naming conventions', 
                     (SELECT COUNT(*) FROM payers WHERE is_cbhi = true);
    ELSE
        RAISE NOTICE 'INFO: is_cbhi column already exists in payers table';
    END IF;
END $$;

-- Verify the column was added
SELECT 
    'Column Verification' as check_type,
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
    END as status;

-- Show data distribution
SELECT 
    'Data Distribution' as info_type,
    is_cbhi,
    COUNT(*) as payer_count
FROM payers 
GROUP BY is_cbhi
ORDER BY is_cbhi;

-- Test the column with a sample query
SELECT 
    'Test Query' as test_type,
    COUNT(*) as total_payers,
    COUNT(*) FILTER (WHERE is_cbhi = true) as cbhi_payers,
    COUNT(*) FILTER (WHERE is_cbhi = false) as non_cbhi_payers
FROM payers;

-- Show any CBHI payers found
SELECT 
    payer_name,
    is_cbhi,
    is_insurance,
    status
FROM payers 
WHERE is_cbhi = true
ORDER BY payer_name;

RAISE NOTICE 'Script completed successfully. The is_cbhi column is now available.';
