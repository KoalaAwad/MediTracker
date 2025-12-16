-- Add 'active' status column to medicine table for soft delete pattern
-- Run this AFTER you've added the Java code changes

-- 1. Add the active column with default value true
ALTER TABLE medicine
ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT true;

-- 2. Set all existing medicines to active
UPDATE medicine
SET active = true
WHERE active IS NULL;

-- 3. Optional: Create an index for better query performance on active medicines
CREATE INDEX IF NOT EXISTS idx_medicine_active ON medicine(active);

-- 4. Optional: See the status of all medicines
-- SELECT medicine_id, medicine_name, active FROM medicine ORDER BY active DESC, medicine_name;

