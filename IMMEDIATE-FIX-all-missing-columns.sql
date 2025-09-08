-- ========================================
-- IMMEDIATE FIX: Add ALL Missing Columns to ALL Tables
-- ========================================
-- This script adds the most commonly missing columns across all tables
-- Run this in your PostgreSQL client to resolve immediate column errors

-- Fix medication_dispensing table (most critical)
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS invoice_number VARCHAR(255);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS batch_code VARCHAR(255);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS dispensing_uuid VARCHAR(255);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS provider_uuid VARCHAR(255);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS payer_uuid VARCHAR(255);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS insured_uuid VARCHAR(255);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS prescription_number VARCHAR(255);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS pharmacy_transaction_id VARCHAR(255);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS dispensing_date DATE;
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS prescribing_physician_name VARCHAR(255);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS prescribing_physician_id VARCHAR(255);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS recorded_at DATE;
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS branch_name VARCHAR(255);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS claim_status VARCHAR(100);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS remark TEXT;
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS status VARCHAR(50);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS claim_uuid VARCHAR(255);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS total_amount DECIMAL(15,2);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS patient_responsibility DECIMAL(15,2);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS insurance_coverage DECIMAL(15,2);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS pharmacist_notes TEXT;
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS source VARCHAR(50);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS batch_id BIGINT;
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS insured_id BIGINT;
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS dependant_id BIGINT;
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT false;
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS primary_diagnosis VARCHAR(500);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS secondary_diagnosis VARCHAR(500);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS attachment_file_name VARCHAR(255);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS attachment_content_type VARCHAR(100);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS attachment_data BYTEA;

-- Fix payers table
ALTER TABLE payers ADD COLUMN IF NOT EXISTS is_cbhi BOOLEAN DEFAULT false NOT NULL;

-- Fix medication_dispensing_item table (if exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'medication_dispensing_item') THEN
        ALTER TABLE medication_dispensing_item ADD COLUMN IF NOT EXISTS item_uuid VARCHAR(255);
        ALTER TABLE medication_dispensing_item ADD COLUMN IF NOT EXISTS medication_code VARCHAR(255);
        ALTER TABLE medication_dispensing_item ADD COLUMN IF NOT EXISTS medication_name VARCHAR(255);
        ALTER TABLE medication_dispensing_item ADD COLUMN IF NOT EXISTS quantity DECIMAL(10,2);
        ALTER TABLE medication_dispensing_item ADD COLUMN IF NOT EXISTS primary_diagnosis VARCHAR(500);
        ALTER TABLE medication_dispensing_item ADD COLUMN IF NOT EXISTS secondary_diagnosis VARCHAR(500);
        ALTER TABLE medication_dispensing_item ADD COLUMN IF NOT EXISTS unit_of_measure VARCHAR(100);
        ALTER TABLE medication_dispensing_item ADD COLUMN IF NOT EXISTS unit_price DECIMAL(15,2);
        ALTER TABLE medication_dispensing_item ADD COLUMN IF NOT EXISTS total_price DECIMAL(15,2);
        ALTER TABLE medication_dispensing_item ADD COLUMN IF NOT EXISTS dosage_instructions TEXT;
        ALTER TABLE medication_dispensing_item ADD COLUMN IF NOT EXISTS strength VARCHAR(255);
        ALTER TABLE medication_dispensing_item ADD COLUMN IF NOT EXISTS route VARCHAR(255);
        ALTER TABLE medication_dispensing_item ADD COLUMN IF NOT EXISTS formulation VARCHAR(255);
        ALTER TABLE medication_dispensing_item ADD COLUMN IF NOT EXISTS item_type VARCHAR(50);
        ALTER TABLE medication_dispensing_item ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT false;
        ALTER TABLE medication_dispensing_item ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
        ALTER TABLE medication_dispensing_item ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
        ALTER TABLE medication_dispensing_item ADD COLUMN IF NOT EXISTS remark TEXT;
        ALTER TABLE medication_dispensing_item ADD COLUMN IF NOT EXISTS contract_detail_id BIGINT;
        ALTER TABLE medication_dispensing_item ADD COLUMN IF NOT EXISTS dispensing_id BIGINT;
    END IF;
END $$;

-- Fix claims table (if exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'claims') THEN
        ALTER TABLE claims ADD COLUMN IF NOT EXISTS claim_uuid VARCHAR(255);
        ALTER TABLE claims ADD COLUMN IF NOT EXISTS claim_number VARCHAR(255);
        ALTER TABLE claims ADD COLUMN IF NOT EXISTS claim_date DATE;
        ALTER TABLE claims ADD COLUMN IF NOT EXISTS total_amount DECIMAL(15,2);
        ALTER TABLE claims ADD COLUMN IF NOT EXISTS status VARCHAR(50);
        ALTER TABLE claims ADD COLUMN IF NOT EXISTS provider_uuid VARCHAR(255);
        ALTER TABLE claims ADD COLUMN IF NOT EXISTS payer_uuid VARCHAR(255);
        ALTER TABLE claims ADD COLUMN IF NOT EXISTS insured_uuid VARCHAR(255);
        ALTER TABLE claims ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT false;
        ALTER TABLE claims ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
        ALTER TABLE claims ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
    END IF;
END $$;

-- Fix batch_records table (if exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'batch_records') THEN
        ALTER TABLE batch_records ADD COLUMN IF NOT EXISTS batch_code VARCHAR(255);
        ALTER TABLE batch_records ADD COLUMN IF NOT EXISTS batch_number BIGINT;
        ALTER TABLE batch_records ADD COLUMN IF NOT EXISTS payer_name VARCHAR(255);
        ALTER TABLE batch_records ADD COLUMN IF NOT EXISTS requested_on DATE;
        ALTER TABLE batch_records ADD COLUMN IF NOT EXISTS claim_dating_from DATE;
        ALTER TABLE batch_records ADD COLUMN IF NOT EXISTS claim_dating_to DATE;
        ALTER TABLE batch_records ADD COLUMN IF NOT EXISTS total_amount DECIMAL(15,2);
        ALTER TABLE batch_records ADD COLUMN IF NOT EXISTS status VARCHAR(100);
        ALTER TABLE batch_records ADD COLUMN IF NOT EXISTS claim_uuid VARCHAR(255);
        ALTER TABLE batch_records ADD COLUMN IF NOT EXISTS rejection_remark TEXT;
        ALTER TABLE batch_records ADD COLUMN IF NOT EXISTS resubmission_remark TEXT;
        ALTER TABLE batch_records ADD COLUMN IF NOT EXISTS rejected_by VARCHAR(255);
        ALTER TABLE batch_records ADD COLUMN IF NOT EXISTS resubmitted_by VARCHAR(255);
        ALTER TABLE batch_records ADD COLUMN IF NOT EXISTS resubmitted_at TIMESTAMP;
        ALTER TABLE batch_records ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP;
        ALTER TABLE batch_records ADD COLUMN IF NOT EXISTS number_of_claims DECIMAL(10,2);
    END IF;
END $$;

-- Fix contract_headers table (if exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'contract_headers') THEN
        ALTER TABLE contract_headers ADD COLUMN IF NOT EXISTS contract_uuid VARCHAR(255);
        ALTER TABLE contract_headers ADD COLUMN IF NOT EXISTS contract_number VARCHAR(255);
        ALTER TABLE contract_headers ADD COLUMN IF NOT EXISTS provider_uuid VARCHAR(255);
        ALTER TABLE contract_headers ADD COLUMN IF NOT EXISTS payer_uuid VARCHAR(255);
        ALTER TABLE contract_headers ADD COLUMN IF NOT EXISTS start_date DATE;
        ALTER TABLE contract_headers ADD COLUMN IF NOT EXISTS end_date DATE;
        ALTER TABLE contract_headers ADD COLUMN IF NOT EXISTS status VARCHAR(50);
        ALTER TABLE contract_headers ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT false;
        ALTER TABLE contract_headers ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
        ALTER TABLE contract_headers ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
    END IF;
END $$;

-- Fix contract_details table (if exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'contract_details') THEN
        ALTER TABLE contract_details ADD COLUMN IF NOT EXISTS contract_detail_uuid VARCHAR(255);
        ALTER TABLE contract_details ADD COLUMN IF NOT EXISTS contract_header_id BIGINT;
        ALTER TABLE contract_details ADD COLUMN IF NOT EXISTS service_id BIGINT;
        ALTER TABLE contract_details ADD COLUMN IF NOT EXISTS service_code VARCHAR(255);
        ALTER TABLE contract_details ADD COLUMN IF NOT EXISTS service_name VARCHAR(255);
        ALTER TABLE contract_details ADD COLUMN IF NOT EXISTS unit_price DECIMAL(15,2);
        ALTER TABLE contract_details ADD COLUMN IF NOT EXISTS coverage_percentage DECIMAL(5,2);
        ALTER TABLE contract_details ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT false;
        ALTER TABLE contract_details ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
        ALTER TABLE contract_details ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
    END IF;
END $$;

-- Add basic indexes for performance
CREATE INDEX IF NOT EXISTS idx_medication_dispensing_batch_code ON medication_dispensing(batch_code);
CREATE INDEX IF NOT EXISTS idx_medication_dispensing_provider_uuid ON medication_dispensing(provider_uuid);
CREATE INDEX IF NOT EXISTS idx_medication_dispensing_payer_uuid ON medication_dispensing(payer_uuid);
CREATE INDEX IF NOT EXISTS idx_medication_dispensing_status ON medication_dispensing(status);
CREATE INDEX IF NOT EXISTS idx_medication_dispensing_deleted ON medication_dispensing(deleted);
CREATE INDEX IF NOT EXISTS idx_payers_is_cbhi ON payers(is_cbhi);

-- Success message
SELECT '✅ SUCCESS: All critical missing columns have been added!' as result;
SELECT '🚀 You can now restart your application - most column errors should be resolved!' as next_step;
SELECT '💡 For complete schema sync, consider running the auto-generation scripts.' as recommendation;
