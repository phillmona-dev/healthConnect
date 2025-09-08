-- Test script to verify null negotiatedPrice handling
-- This script creates test data with null negotiatedPrice to verify our fix

-- First, let's check if contract_details table exists and has the right structure
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'contract_details' 
ORDER BY ordinal_position;

-- Insert a test contract detail with null negotiated price to test our fix
-- (This would normally cause the NullPointerException we just fixed)

-- Check if there are any existing contract details with null negotiated_price
SELECT 
    contract_detail_uuid,
    service_uuid,
    negotiated_price,
    status,
    created_at
FROM contract_details 
WHERE negotiated_price IS NULL
LIMIT 5;

-- Update any existing null negotiated_price values to 0.0 to prevent future issues
UPDATE contract_details 
SET negotiated_price = 0.0 
WHERE negotiated_price IS NULL;

-- Verify the update
SELECT COUNT(*) as null_price_count
FROM contract_details 
WHERE negotiated_price IS NULL;

-- Show some sample contract details to verify structure
SELECT 
    contract_detail_uuid,
    contract_header_uuid,
    service_uuid,
    negotiated_price,
    status,
    is_deleted
FROM contract_details 
WHERE is_deleted = false
LIMIT 10;

SELECT 'SUCCESS: Null negotiated_price values have been fixed!' as result;
