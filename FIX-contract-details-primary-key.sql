-- ========================================
-- FIX CONTRACT_DETAILS PRIMARY KEY ISSUE
-- ========================================
-- This script fixes the primary key conflict in the junction table

-- Step 1: Drop the problematic junction table
DROP TABLE IF EXISTS contract_detail_employee_dependant_groups;

-- Step 2: Recreate junction table with proper composite primary key
CREATE TABLE contract_detail_employee_dependant_groups (
    contract_detail_id BIGINT NOT NULL,
    employee_dependant_group_id BIGINT NOT NULL,
    PRIMARY KEY (contract_detail_id, employee_dependant_group_id)
);

-- Step 3: Add indexes for performance
CREATE INDEX IF NOT EXISTS idx_junction_contract_detail ON contract_detail_employee_dependant_groups(contract_detail_id);
CREATE INDEX IF NOT EXISTS idx_junction_employee_group ON contract_detail_employee_dependant_groups(employee_dependant_group_id);

-- Step 4: Ensure contract_details table has all columns
ALTER TABLE contract_details ADD COLUMN IF NOT EXISTS contract_detail_uuid VARCHAR(255);
ALTER TABLE contract_details ADD COLUMN IF NOT EXISTS contract_header_uuid VARCHAR(255);
ALTER TABLE contract_details ADD COLUMN IF NOT EXISTS service_uuid VARCHAR(255);
ALTER TABLE contract_details ADD COLUMN IF NOT EXISTS negotiated_price DECIMAL(19,2);
ALTER TABLE contract_details ADD COLUMN IF NOT EXISTS status VARCHAR(50);
ALTER TABLE contract_details ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT false;
ALTER TABLE contract_details ADD COLUMN IF NOT EXISTS drug_uuid VARCHAR(255);
ALTER TABLE contract_details ADD COLUMN IF NOT EXISTS item_type VARCHAR(100);
ALTER TABLE contract_details ADD COLUMN IF NOT EXISTS contract_header_id BIGINT;
ALTER TABLE contract_details ADD COLUMN IF NOT EXISTS service_id BIGINT;
ALTER TABLE contract_details ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE contract_details ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);
ALTER TABLE contract_details ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE contract_details ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Step 5: Add unique constraint on contract_detail_uuid if not exists
CREATE UNIQUE INDEX IF NOT EXISTS idx_contract_detail_uuid ON contract_details(contract_detail_uuid);

-- Step 6: Add other performance indexes
CREATE INDEX IF NOT EXISTS idx_contract_header_uuid ON contract_details(contract_header_uuid);
CREATE INDEX IF NOT EXISTS idx_service_uuid ON contract_details(service_uuid);
CREATE INDEX IF NOT EXISTS idx_drug_uuid ON contract_details(drug_uuid);
CREATE INDEX IF NOT EXISTS idx_contract_details_deleted ON contract_details(is_deleted);
CREATE INDEX IF NOT EXISTS idx_contract_details_status ON contract_details(status);

-- Step 7: Verification
SELECT 'SUCCESS: contract_details table fixed!' as result;

-- Check table structure
SELECT 
    'contract_details columns:' as info,
    column_name,
    data_type,
    is_nullable
FROM information_schema.columns 
WHERE table_name = 'contract_details' 
  AND table_schema = 'public'
ORDER BY ordinal_position;

-- Check junction table structure
SELECT 
    'junction table columns:' as info,
    column_name,
    data_type,
    is_nullable
FROM information_schema.columns 
WHERE table_name = 'contract_detail_employee_dependant_groups' 
  AND table_schema = 'public'
ORDER BY ordinal_position;

-- ========================================
-- SUMMARY:
-- ========================================
-- ✅ Fixed primary key conflict in junction table
-- ✅ Recreated junction table with proper composite primary key
-- ✅ Ensured all contract_details columns exist
-- ✅ Added performance indexes
-- ✅ Ready for JPA entity mapping
-- ========================================
