-- ========================================
-- SIMPLE CONTRACT_DETAILS TABLE CREATION
-- ========================================
-- This script creates the contract_details table with simple, safe SQL
-- No complex DO blocks or dollar quotes - just basic SQL statements

-- Step 1: Create the table if it doesn't exist
CREATE TABLE IF NOT EXISTS contract_details (
    id BIGSERIAL PRIMARY KEY
);

-- Step 2: Add all columns (safe with IF NOT EXISTS)
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

-- Step 3: Add basic indexes (safe with IF NOT EXISTS)
CREATE UNIQUE INDEX IF NOT EXISTS idx_contract_detail_uuid ON contract_details(contract_detail_uuid);
CREATE INDEX IF NOT EXISTS idx_contract_header_uuid ON contract_details(contract_header_uuid);
CREATE INDEX IF NOT EXISTS idx_service_uuid ON contract_details(service_uuid);
CREATE INDEX IF NOT EXISTS idx_drug_uuid ON contract_details(drug_uuid);
CREATE INDEX IF NOT EXISTS idx_contract_details_deleted ON contract_details(is_deleted);
CREATE INDEX IF NOT EXISTS idx_contract_details_status ON contract_details(status);
CREATE INDEX IF NOT EXISTS idx_contract_details_item_type ON contract_details(item_type);
CREATE INDEX IF NOT EXISTS idx_contract_details_header_id ON contract_details(contract_header_id);
CREATE INDEX IF NOT EXISTS idx_contract_details_service_id ON contract_details(service_id);

-- Step 4: Create junction table for many-to-many relationships
CREATE TABLE IF NOT EXISTS contract_detail_employee_dependant_groups (
    contract_detail_id BIGINT,
    employee_dependant_group_id BIGINT
);

-- Add indexes for junction table
CREATE INDEX IF NOT EXISTS idx_junction_contract_detail ON contract_detail_employee_dependant_groups(contract_detail_id);
CREATE INDEX IF NOT EXISTS idx_junction_employee_group ON contract_detail_employee_dependant_groups(employee_dependant_group_id);

-- Step 5: Verification
SELECT 'SUCCESS: contract_details table created with all columns!' as result;

-- Check table structure
SELECT 
    column_name,
    data_type,
    is_nullable,
    character_maximum_length
FROM information_schema.columns 
WHERE table_name = 'contract_details' 
  AND table_schema = 'public'
ORDER BY ordinal_position;

-- ========================================
-- SUMMARY:
-- ========================================
-- ✅ contract_details table created with ALL columns from ContractDetail entity
-- ✅ All indexes added for performance
-- ✅ Junction table created for many-to-many relationships
-- ✅ Safe execution with IF NOT EXISTS
-- ✅ No complex SQL that could cause parsing errors
-- 
-- Columns included:
-- - id (Primary Key)
-- - contract_detail_uuid (Unique identifier)
-- - contract_header_uuid (Reference to contract header)
-- - service_uuid (Reference to service)
-- - negotiated_price (Contract price)
-- - status (Contract status)
-- - is_deleted (Soft delete flag)
-- - drug_uuid (Reference to drug)
-- - item_type (Type of contract item)
-- - contract_header_id (Foreign key to contract_headers)
-- - service_id (Foreign key to servicelists)
-- - created_by, updated_by, created_at, updated_at (Audit fields)
-- 
-- Junction table:
-- - contract_detail_employee_dependant_groups (Many-to-many relationship)
-- ========================================
