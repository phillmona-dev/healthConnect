-- Complete SQL script to add ALL missing columns to medication_dispensing table
-- This script ensures the table matches the MedicationDispensing entity exactly

-- ========================================
-- 1. Add ALL missing columns
-- ========================================

DO $$
BEGIN
    RAISE NOTICE 'Starting medication_dispensing table completion...';
    
    -- Core dispensing information columns
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'invoice_number') THEN
        ALTER TABLE medication_dispensing ADD COLUMN invoice_number VARCHAR(255);
        RAISE NOTICE 'Added invoice_number column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'batch_code') THEN
        ALTER TABLE medication_dispensing ADD COLUMN batch_code VARCHAR(255);
        RAISE NOTICE 'Added batch_code column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'dispensing_uuid') THEN
        ALTER TABLE medication_dispensing ADD COLUMN dispensing_uuid VARCHAR(255);
        RAISE NOTICE 'Added dispensing_uuid column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'provider_uuid') THEN
        ALTER TABLE medication_dispensing ADD COLUMN provider_uuid VARCHAR(255);
        RAISE NOTICE 'Added provider_uuid column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'payer_uuid') THEN
        ALTER TABLE medication_dispensing ADD COLUMN payer_uuid VARCHAR(255);
        RAISE NOTICE 'Added payer_uuid column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'insured_uuid') THEN
        ALTER TABLE medication_dispensing ADD COLUMN insured_uuid VARCHAR(255);
        RAISE NOTICE 'Added insured_uuid column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'prescription_number') THEN
        ALTER TABLE medication_dispensing ADD COLUMN prescription_number VARCHAR(255);
        RAISE NOTICE 'Added prescription_number column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'pharmacy_transaction_id') THEN
        ALTER TABLE medication_dispensing ADD COLUMN pharmacy_transaction_id VARCHAR(255);
        RAISE NOTICE 'Added pharmacy_transaction_id column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'dispensing_date') THEN
        ALTER TABLE medication_dispensing ADD COLUMN dispensing_date DATE;
        RAISE NOTICE 'Added dispensing_date column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'prescribing_physician_name') THEN
        ALTER TABLE medication_dispensing ADD COLUMN prescribing_physician_name VARCHAR(255);
        RAISE NOTICE 'Added prescribing_physician_name column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'prescribing_physician_id') THEN
        ALTER TABLE medication_dispensing ADD COLUMN prescribing_physician_id VARCHAR(255);
        RAISE NOTICE 'Added prescribing_physician_id column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'recorded_at') THEN
        ALTER TABLE medication_dispensing ADD COLUMN recorded_at DATE;
        RAISE NOTICE 'Added recorded_at column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'branch_name') THEN
        ALTER TABLE medication_dispensing ADD COLUMN branch_name VARCHAR(255);
        RAISE NOTICE 'Added branch_name column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'claim_status') THEN
        ALTER TABLE medication_dispensing ADD COLUMN claim_status VARCHAR(100);
        RAISE NOTICE 'Added claim_status column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'remark') THEN
        ALTER TABLE medication_dispensing ADD COLUMN remark TEXT;
        RAISE NOTICE 'Added remark column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'status') THEN
        ALTER TABLE medication_dispensing ADD COLUMN status VARCHAR(50);
        RAISE NOTICE 'Added status column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'claim_uuid') THEN
        ALTER TABLE medication_dispensing ADD COLUMN claim_uuid VARCHAR(255);
        RAISE NOTICE 'Added claim_uuid column';
    END IF;
    
    -- Financial columns
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'total_amount') THEN
        ALTER TABLE medication_dispensing ADD COLUMN total_amount DECIMAL(15,2);
        RAISE NOTICE 'Added total_amount column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'patient_responsibility') THEN
        ALTER TABLE medication_dispensing ADD COLUMN patient_responsibility DECIMAL(15,2);
        RAISE NOTICE 'Added patient_responsibility column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'insurance_coverage') THEN
        ALTER TABLE medication_dispensing ADD COLUMN insurance_coverage DECIMAL(15,2);
        RAISE NOTICE 'Added insurance_coverage column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'pharmacist_notes') THEN
        ALTER TABLE medication_dispensing ADD COLUMN pharmacist_notes TEXT;
        RAISE NOTICE 'Added pharmacist_notes column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'source') THEN
        ALTER TABLE medication_dispensing ADD COLUMN source VARCHAR(50);
        RAISE NOTICE 'Added source column';
    END IF;
    
    -- Foreign key columns
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'batch_id') THEN
        ALTER TABLE medication_dispensing ADD COLUMN batch_id BIGINT;
        RAISE NOTICE 'Added batch_id column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'insured_id') THEN
        ALTER TABLE medication_dispensing ADD COLUMN insured_id BIGINT;
        RAISE NOTICE 'Added insured_id column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'dependant_id') THEN
        ALTER TABLE medication_dispensing ADD COLUMN dependant_id BIGINT;
        RAISE NOTICE 'Added dependant_id column';
    END IF;
    
    -- System columns
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'deleted') THEN
        ALTER TABLE medication_dispensing ADD COLUMN deleted BOOLEAN DEFAULT false NOT NULL;
        RAISE NOTICE 'Added deleted column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'created_at') THEN
        ALTER TABLE medication_dispensing ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
        RAISE NOTICE 'Added created_at column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'updated_at') THEN
        ALTER TABLE medication_dispensing ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
        RAISE NOTICE 'Added updated_at column';
    END IF;
    
    -- Diagnosis columns
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'primary_diagnosis') THEN
        ALTER TABLE medication_dispensing ADD COLUMN primary_diagnosis VARCHAR(500);
        RAISE NOTICE 'Added primary_diagnosis column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'secondary_diagnosis') THEN
        ALTER TABLE medication_dispensing ADD COLUMN secondary_diagnosis VARCHAR(500);
        RAISE NOTICE 'Added secondary_diagnosis column';
    END IF;
    
    -- Attachment columns
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'attachment_file_name') THEN
        ALTER TABLE medication_dispensing ADD COLUMN attachment_file_name VARCHAR(255);
        RAISE NOTICE 'Added attachment_file_name column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'attachment_content_type') THEN
        ALTER TABLE medication_dispensing ADD COLUMN attachment_content_type VARCHAR(100);
        RAISE NOTICE 'Added attachment_content_type column';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medication_dispensing' AND column_name = 'attachment_data') THEN
        ALTER TABLE medication_dispensing ADD COLUMN attachment_data BYTEA;
        RAISE NOTICE 'Added attachment_data column';
    END IF;
    
    RAISE NOTICE 'All columns added successfully!';
END $$;

-- ========================================
-- 2. Add indexes and constraints
-- ========================================

DO $$
BEGIN
    RAISE NOTICE 'Adding indexes and constraints...';

    -- Add unique constraints
    BEGIN
        ALTER TABLE medication_dispensing ADD CONSTRAINT uk_medication_dispensing_uuid UNIQUE (dispensing_uuid);
        RAISE NOTICE 'Added unique constraint on dispensing_uuid';
    EXCEPTION WHEN duplicate_table THEN
        RAISE NOTICE 'Unique constraint on dispensing_uuid already exists';
    END;

    BEGIN
        ALTER TABLE medication_dispensing ADD CONSTRAINT uk_medication_dispensing_pharmacy_tx UNIQUE (pharmacy_transaction_id);
        RAISE NOTICE 'Added unique constraint on pharmacy_transaction_id';
    EXCEPTION WHEN duplicate_table THEN
        RAISE NOTICE 'Unique constraint on pharmacy_transaction_id already exists';
    END;

    -- Add performance indexes
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE tablename = 'medication_dispensing' AND indexname = 'idx_medication_dispensing_provider_uuid') THEN
        CREATE INDEX idx_medication_dispensing_provider_uuid ON medication_dispensing(provider_uuid);
        RAISE NOTICE 'Added index on provider_uuid';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE tablename = 'medication_dispensing' AND indexname = 'idx_medication_dispensing_payer_uuid') THEN
        CREATE INDEX idx_medication_dispensing_payer_uuid ON medication_dispensing(payer_uuid);
        RAISE NOTICE 'Added index on payer_uuid';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE tablename = 'medication_dispensing' AND indexname = 'idx_medication_dispensing_insured_uuid') THEN
        CREATE INDEX idx_medication_dispensing_insured_uuid ON medication_dispensing(insured_uuid);
        RAISE NOTICE 'Added index on insured_uuid';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE tablename = 'medication_dispensing' AND indexname = 'idx_medication_dispensing_status') THEN
        CREATE INDEX idx_medication_dispensing_status ON medication_dispensing(status);
        RAISE NOTICE 'Added index on status';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE tablename = 'medication_dispensing' AND indexname = 'idx_medication_dispensing_dispensing_date') THEN
        CREATE INDEX idx_medication_dispensing_dispensing_date ON medication_dispensing(dispensing_date);
        RAISE NOTICE 'Added index on dispensing_date';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE tablename = 'medication_dispensing' AND indexname = 'idx_medication_dispensing_claim_status') THEN
        CREATE INDEX idx_medication_dispensing_claim_status ON medication_dispensing(claim_status);
        RAISE NOTICE 'Added index on claim_status';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE tablename = 'medication_dispensing' AND indexname = 'idx_medication_dispensing_deleted') THEN
        CREATE INDEX idx_medication_dispensing_deleted ON medication_dispensing(deleted);
        RAISE NOTICE 'Added index on deleted';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE tablename = 'medication_dispensing' AND indexname = 'idx_medication_dispensing_batch_code') THEN
        CREATE INDEX idx_medication_dispensing_batch_code ON medication_dispensing(batch_code);
        RAISE NOTICE 'Added index on batch_code';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE tablename = 'medication_dispensing' AND indexname = 'idx_medication_dispensing_claim_uuid') THEN
        CREATE INDEX idx_medication_dispensing_claim_uuid ON medication_dispensing(claim_uuid);
        RAISE NOTICE 'Added index on claim_uuid';
    END IF;

    RAISE NOTICE 'All indexes and constraints added successfully!';
END $$;

-- ========================================
-- 3. Verification
-- ========================================

-- Show all columns in the table
SELECT
    'MEDICATION_DISPENSING TABLE STRUCTURE' as info_type,
    column_name,
    data_type,
    is_nullable,
    column_default,
    character_maximum_length
FROM information_schema.columns
WHERE table_name = 'medication_dispensing'
  AND table_schema = 'public'
ORDER BY ordinal_position;

-- Show indexes
SELECT
    'MEDICATION_DISPENSING INDEXES' as info_type,
    indexname,
    indexdef
FROM pg_indexes
WHERE tablename = 'medication_dispensing'
ORDER BY indexname;

-- Test query that was failing
SELECT
    'TEST QUERY RESULT' as test_type,
    COUNT(*) as total_records
FROM medication_dispensing;

-- Final success message
DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE '✅ MEDICATION_DISPENSING TABLE COMPLETED!';
    RAISE NOTICE '========================================';
    RAISE NOTICE '✅ All entity columns are now present';
    RAISE NOTICE '✅ All indexes created for performance';
    RAISE NOTICE '✅ All constraints added for data integrity';
    RAISE NOTICE '';
    RAISE NOTICE '🚀 You can now restart your application!';
    RAISE NOTICE '🚀 All column errors should be resolved:';
    RAISE NOTICE '   - "column md1_0.batch_code does not exist"';
    RAISE NOTICE '   - "column md1_0.attachment_content_type does not exist"';
    RAISE NOTICE '   - And any other missing column errors';
    RAISE NOTICE '========================================';
END $$;
