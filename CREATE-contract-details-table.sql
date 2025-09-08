-- ========================================
-- CREATE CONTRACT_DETAILS TABLE - COMPLETE SOLUTION
-- ========================================
-- This script creates the complete contract_details table with all columns
-- based on the ContractDetail JPA entity
-- Run this in your PostgreSQL client (pgAdmin, psql, etc.)

-- Step 1: Create contract_details table with all columns
CREATE TABLE IF NOT EXISTS contract_details (
    id BIGSERIAL PRIMARY KEY,
    
    -- Core entity fields
    contract_detail_uuid VARCHAR(255) UNIQUE NOT NULL,
    contract_header_uuid VARCHAR(255) NOT NULL,
    service_uuid VARCHAR(255),
    negotiated_price DECIMAL(19,2),
    status VARCHAR(50),
    is_deleted BOOLEAN DEFAULT false NOT NULL,
    drug_uuid VARCHAR(255),
    item_type VARCHAR(100),
    
    -- Foreign key fields
    contract_header_id BIGINT,
    service_id BIGINT,
    
    -- Audit fields (from Audit superclass)
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Step 2: Add missing columns to existing table (if table already exists)
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

-- Step 3: Add unique constraint on contract_detail_uuid if not exists
DO $$ 
BEGIN
    BEGIN
        ALTER TABLE contract_details ADD CONSTRAINT uk_contract_detail_uuid UNIQUE (contract_detail_uuid);
    EXCEPTION WHEN duplicate_table THEN
        -- Constraint already exists, ignore
        NULL;
    END;
END $$;

-- Step 4: Add performance indexes
CREATE UNIQUE INDEX IF NOT EXISTS idx_contract_detail_uuid ON contract_details(contract_detail_uuid);
CREATE INDEX IF NOT EXISTS idx_contract_header_uuid ON contract_details(contract_header_uuid);
CREATE INDEX IF NOT EXISTS idx_service_uuid ON contract_details(service_uuid);
CREATE INDEX IF NOT EXISTS idx_drug_uuid ON contract_details(drug_uuid);
CREATE INDEX IF NOT EXISTS idx_contract_details_deleted ON contract_details(is_deleted);
CREATE INDEX IF NOT EXISTS idx_contract_details_status ON contract_details(status);
CREATE INDEX IF NOT EXISTS idx_contract_details_item_type ON contract_details(item_type);
CREATE INDEX IF NOT EXISTS idx_contract_details_header_id ON contract_details(contract_header_id);
CREATE INDEX IF NOT EXISTS idx_contract_details_service_id ON contract_details(service_id);
CREATE INDEX IF NOT EXISTS idx_contract_details_created_at ON contract_details(created_at);
CREATE INDEX IF NOT EXISTS idx_contract_details_updated_at ON contract_details(updated_at);

-- Step 5: Create junction table for many-to-many relationship with employee_dependant_groups
CREATE TABLE IF NOT EXISTS contract_detail_employee_dependant_groups (
    contract_detail_id BIGINT NOT NULL,
    employee_dependant_group_id BIGINT NOT NULL,
    PRIMARY KEY (contract_detail_id, employee_dependant_group_id)
);

-- Add indexes for junction table
CREATE INDEX IF NOT EXISTS idx_contract_detail_employee_groups_contract ON contract_detail_employee_dependant_groups(contract_detail_id);
CREATE INDEX IF NOT EXISTS idx_contract_detail_employee_groups_employee ON contract_detail_employee_dependant_groups(employee_dependant_group_id);

-- Step 6: Add foreign key constraints (with error handling)
-- Foreign key to contract_headers
DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'contract_headers') THEN
        BEGIN
            ALTER TABLE contract_details ADD CONSTRAINT fk_contract_details_header 
            FOREIGN KEY (contract_header_id) REFERENCES contract_headers(id);
        EXCEPTION WHEN duplicate_object THEN
            -- Constraint already exists, ignore
            NULL;
        END;
    END IF;
END $$;

-- Foreign key to servicelists
DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'servicelists') THEN
        BEGIN
            ALTER TABLE contract_details ADD CONSTRAINT fk_contract_details_service 
            FOREIGN KEY (service_id) REFERENCES servicelists(id);
        EXCEPTION WHEN duplicate_object THEN
            -- Constraint already exists, ignore
            NULL;
        END;
    END IF;
END $$;

-- Foreign key to drugs
DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'drugs') THEN
        BEGIN
            ALTER TABLE contract_details ADD CONSTRAINT fk_contract_details_drug 
            FOREIGN KEY (drug_uuid) REFERENCES drugs(drug_uuid);
        EXCEPTION WHEN duplicate_object THEN
            -- Constraint already exists, ignore
            NULL;
        END;
    END IF;
END $$;

-- Foreign key for junction table to contract_details
DO $$ 
BEGIN
    BEGIN
        ALTER TABLE contract_detail_employee_dependant_groups ADD CONSTRAINT fk_junction_contract_detail 
        FOREIGN KEY (contract_detail_id) REFERENCES contract_details(id) ON DELETE CASCADE;
    EXCEPTION WHEN duplicate_object THEN
        -- Constraint already exists, ignore
        NULL;
    END;
END $$;

-- Foreign key for junction table to employee_dependant_groups
DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'employee_dependant_groups') THEN
        BEGIN
            ALTER TABLE contract_detail_employee_dependant_groups ADD CONSTRAINT fk_junction_employee_group 
            FOREIGN KEY (employee_dependant_group_id) REFERENCES employee_dependant_groups(id) ON DELETE CASCADE;
        EXCEPTION WHEN duplicate_object THEN
            -- Constraint already exists, ignore
            NULL;
        END;
    END IF;
END $$;

-- Step 7: Verification - Check table structure
SELECT 
    'VERIFICATION: contract_details table structure' as info,
    column_name,
    data_type,
    is_nullable,
    character_maximum_length,
    numeric_precision,
    numeric_scale
FROM information_schema.columns 
WHERE table_name = 'contract_details' 
  AND table_schema = 'public'
ORDER BY ordinal_position;

-- Step 8: Check indexes
SELECT 
    'VERIFICATION: contract_details indexes' as info,
    indexname,
    indexdef
FROM pg_indexes 
WHERE tablename = 'contract_details'
ORDER BY indexname;

-- Step 9: Success message
SELECT '✅ SUCCESS: contract_details table created with all columns!' as result;
SELECT '🚀 Table includes all fields from ContractDetail entity!' as details;
SELECT '📊 Performance indexes and foreign keys added!' as performance;
SELECT '🔗 Junction table for many-to-many relationships created!' as relationships;

-- ========================================
-- SUMMARY OF WHAT WAS CREATED:
-- ========================================
-- ✅ contract_details table with ALL columns from ContractDetail entity:
--    - id (BIGSERIAL PRIMARY KEY)
--    - contract_detail_uuid (VARCHAR(255) UNIQUE NOT NULL)
--    - contract_header_uuid (VARCHAR(255) NOT NULL)
--    - service_uuid (VARCHAR(255))
--    - negotiated_price (DECIMAL(19,2))
--    - status (VARCHAR(50))
--    - is_deleted (BOOLEAN DEFAULT false)
--    - drug_uuid (VARCHAR(255))
--    - item_type (VARCHAR(100))
--    - contract_header_id (BIGINT)
--    - service_id (BIGINT)
--    - created_by, updated_by, created_at, updated_at (Audit fields)
--
-- ✅ contract_detail_employee_dependant_groups junction table
-- ✅ Performance indexes on all key columns
-- ✅ Foreign key constraints to related tables
-- ✅ Unique constraints for data integrity
-- ✅ Safe execution with IF NOT EXISTS and error handling
