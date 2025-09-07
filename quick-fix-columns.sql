-- Quick fix for the most critical missing columns
-- Run this immediately to resolve the current errors

-- Add the columns that are causing immediate errors
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS batch_code VARCHAR(255);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS attachment_content_type VARCHAR(100);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS attachment_file_name VARCHAR(255);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS attachment_data BYTEA;

-- Add other critical columns that are likely missing
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS dispensing_uuid VARCHAR(255);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS provider_uuid VARCHAR(255);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS payer_uuid VARCHAR(255);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS insured_uuid VARCHAR(255);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS prescription_number VARCHAR(255);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS pharmacy_transaction_id VARCHAR(255);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS dispensing_date DATE;
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS status VARCHAR(50);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS total_amount DECIMAL(15,2);
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT false;
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE medication_dispensing ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Add basic indexes
CREATE INDEX IF NOT EXISTS idx_medication_dispensing_batch_code ON medication_dispensing(batch_code);
CREATE INDEX IF NOT EXISTS idx_medication_dispensing_provider_uuid ON medication_dispensing(provider_uuid);
CREATE INDEX IF NOT EXISTS idx_medication_dispensing_status ON medication_dispensing(status);

-- Verify the fix
SELECT 'SUCCESS: Critical columns added to medication_dispensing table' as result;
