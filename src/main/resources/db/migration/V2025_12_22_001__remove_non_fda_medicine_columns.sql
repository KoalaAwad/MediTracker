-- Drop unused columns from medicine table that aren't part of FDA data
ALTER TABLE medicine
DROP COLUMN IF EXISTS dosage_form,
DROP COLUMN IF EXISTS strength,
DROP COLUMN IF EXISTS description,
DROP COLUMN IF EXISTS side_effects,
DROP COLUMN IF EXISTS contraindications;

