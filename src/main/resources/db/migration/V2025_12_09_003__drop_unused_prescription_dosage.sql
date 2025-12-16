-- Clean up unused prescription_dosage table
-- This table is no longer used - replaced by embedded Dosage and prescription_schedule

-- 1. Drop the prescription_dosage table (if it exists and is empty)
DROP TABLE IF EXISTS prescription_dosage CASCADE;

-- 2. Remove the PrescriptionDosage entity file manually:
-- Delete: src/main/java/org/springbozo/meditracker/model/PrescriptionDosage.java

-- Note: This table was part of an older design that has been superseded by:
-- - Dosage embeddable (in Prescription entity)
-- - prescription_schedule table (for schedule entries)

