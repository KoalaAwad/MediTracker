-- Adds a `openfda` jsonb column to store OpenFDA array fields exactly
-- and creates a GIN index for efficient searches.

ALTER TABLE medicine
ADD COLUMN IF NOT EXISTS openfda jsonb;

-- Create GIN index for containment/search on openfda
CREATE INDEX IF NOT EXISTS idx_medicine_openfda_gin ON medicine USING GIN (openfda jsonb_path_ops);

-- Optional: set empty JSON object for existing rows to simplify queries
UPDATE medicine SET openfda = '{}'::jsonb WHERE openfda IS NULL;

