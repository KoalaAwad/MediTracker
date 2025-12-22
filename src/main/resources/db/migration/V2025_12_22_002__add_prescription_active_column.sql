-- Add active column to prescription table for soft delete
ALTER TABLE prescription
ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

-- Create index for filtering active prescriptions
CREATE INDEX IF NOT EXISTS idx_prescription_active ON prescription(active);

