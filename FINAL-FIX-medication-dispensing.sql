-- ========================================
-- FINAL COMPLETE FIX FOR MEDICATION_DISPENSING TABLE
-- ========================================
-- This script adds ALL missing columns to resolve column errors
-- Run this in your PostgreSQL client (pgAdmin, psql, etc.)

-- Step 1: Add ALL missing columns (safe with IF NOT EXISTS)
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

-- Step 2: Add performance indexes (safe with IF NOT EXISTS)
CREATE INDEX IF NOT EXISTS idx_medication_dispensing_batch_code ON medication_dispensing(batch_code);
CREATE INDEX IF NOT EXISTS idx_medication_dispensing_provider_uuid ON medication_dispensing(provider_uuid);
CREATE INDEX IF NOT EXISTS idx_medication_dispensing_payer_uuid ON medication_dispensing(payer_uuid);
CREATE INDEX IF NOT EXISTS idx_medication_dispensing_insured_uuid ON medication_dispensing(insured_uuid);
CREATE INDEX IF NOT EXISTS idx_medication_dispensing_status ON medication_dispensing(status);
CREATE INDEX IF NOT EXISTS idx_medication_dispensing_dispensing_date ON medication_dispensing(dispensing_date);
CREATE INDEX IF NOT EXISTS idx_medication_dispensing_claim_status ON medication_dispensing(claim_status);
CREATE INDEX IF NOT EXISTS idx_medication_dispensing_deleted ON medication_dispensing(deleted);
CREATE INDEX IF NOT EXISTS idx_medication_dispensing_claim_uuid ON medication_dispensing(claim_uuid);
CREATE INDEX IF NOT EXISTS idx_medication_dispensing_attachment_file ON medication_dispensing(attachment_file_name);

-- Step 3: Add unique constraints (with error handling)
DO $$ 
BEGIN
    BEGIN
        ALTER TABLE medication_dispensing ADD CONSTRAINT uk_medication_dispensing_uuid UNIQUE (dispensing_uuid);
    EXCEPTION WHEN duplicate_table THEN
        -- Constraint already exists, ignore
        NULL;
    END;
END $$;

DO $$ 
BEGIN
    BEGIN
        ALTER TABLE medication_dispensing ADD CONSTRAINT uk_medication_dispensing_pharmacy_tx UNIQUE (pharmacy_transaction_id);
    EXCEPTION WHEN duplicate_table THEN
        -- Constraint already exists, ignore
        NULL;
    END;
END $$;

-- Step 4: Verification - Check all columns exist
SELECT 
    'VERIFICATION: medication_dispensing table structure' as info,
    column_name,
    data_type,
    is_nullable,
    character_maximum_length
FROM information_schema.columns 
WHERE table_name = 'medication_dispensing' 
  AND table_schema = 'public'
ORDER BY ordinal_position;

-- Step 5: Test query that was failing
SELECT 
    'TEST: Sample query' as test_type,
    COUNT(*) as total_records
FROM medication_dispensing;

-- Step 6: Success message
SELECT '✅ SUCCESS: All columns added to medication_dispensing table!' as result;
SELECT '🚀 You can now restart your application - all column errors should be resolved!' as next_step;

-- ========================================
-- SUMMARY OF WHAT WAS ADDED:
-- ========================================
-- Core columns: invoice_number, batch_code, dispensing_uuid, provider_uuid, payer_uuid, insured_uuid
-- Transaction columns: prescription_number, pharmacy_transaction_id, dispensing_date
-- Personnel columns: prescribing_physician_name, prescribing_physician_id
-- Status columns: recorded_at, branch_name, claim_status, remark, status, claim_uuid
-- Financial columns: total_amount, patient_responsibility, insurance_coverage, pharmacist_notes, source
-- Relationship columns: batch_id, insured_id, dependant_id
-- System columns: deleted, created_at, updated_at
-- Medical columns: primary_diagnosis, secondary_diagnosis
-- Attachment columns: attachment_file_name, attachment_content_type, attachment_data
-- Performance indexes on all key columns
-- Unique constraints on dispensing_uuid and pharmacy_transaction_id
