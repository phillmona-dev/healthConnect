-- ========================================
-- Debug ID Number Search Issue
-- ========================================
-- Run this SQL in pgAdmin to investigate the issue

-- Step 1: Find ALL insured persons with idNumber = '557'
SELECT 
    id,
    insured_uuid,
    first_name,
    father_name,
    grand_father_name,
    id_number,
    employee_id,
    insurance_id,
    national_id,
    phone,
    payer_uuid,
    is_deleted,
    created_at
FROM insured 
WHERE id_number = '557'
ORDER BY id ASC;

-- Step 2: Find the person that's being returned (Shibiru)
SELECT 
    id,
    insured_uuid,
    first_name,
    father_name,
    grand_father_name,
    id_number,
    employee_id,
    insurance_id,
    national_id,
    phone,
    payer_uuid,
    is_deleted
FROM insured 
WHERE insured_uuid = '472644e0-f0ea-4e5c-872e-8f329ecd1add';

-- Step 3: Find Abeba Afework
SELECT 
    id,
    insured_uuid,
    first_name,
    father_name,
    grand_father_name,
    id_number,
    employee_id,
    insurance_id,
    national_id,
    phone,
    payer_uuid,
    is_deleted
FROM insured 
WHERE first_name LIKE '%Abeba%' 
  AND father_name LIKE '%Afework%';

-- Step 4: Check if there are multiple people with idNumber = '557'
SELECT 
    id_number,
    COUNT(*) as count,
    STRING_AGG(DISTINCT first_name || ' ' || father_name, ', ') as names,
    STRING_AGG(DISTINCT payer_uuid, ', ') as payers
FROM insured 
WHERE id_number = '557'
  AND is_deleted = false
GROUP BY id_number;

-- Step 5: Check for data quality issues - find all unique idNumbers
SELECT 
    id_number,
    COUNT(*) as count
FROM insured 
WHERE is_deleted = false
  AND id_number IS NOT NULL
GROUP BY id_number
HAVING COUNT(*) > 1
ORDER BY count DESC
LIMIT 20;

-- Step 6: Check if the columns are mixed up
SELECT 
    id,
    first_name,
    father_name,
    grand_father_name,
    id_number,
    phone
FROM insured 
WHERE phone = '557' 
   OR grand_father_name = '557'
   OR id_number = '557'
ORDER BY id ASC;

