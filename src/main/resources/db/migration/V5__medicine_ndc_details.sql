-- Medicines now come from the openFDA NDC directory: one row per product
-- (brand + strength + form), e.g. "Prograf 1 mg capsule" and "Prograf 5 mg capsule" are separate.
ALTER TABLE medicine
    ADD COLUMN product_ndc VARCHAR(20) UNIQUE,
    ADD COLUMN strength VARCHAR(500),
    ADD COLUMN dosage_form VARCHAR(150),
    ADD COLUMN route VARCHAR(150),
    ADD COLUMN product_type VARCHAR(60),
    ADD COLUMN spl_set_id VARCHAR(40),
    ADD COLUMN last_synced_at TIMESTAMP;

ALTER TABLE medicine ALTER COLUMN generic_name TYPE VARCHAR(1000);
ALTER TABLE medicine ALTER COLUMN manufacturer TYPE VARCHAR(500);

-- substring search over ~115k names
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX IF NOT EXISTS idx_medicine_name_trgm ON medicine USING gin (lower(medicine_name) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_medicine_generic_trgm ON medicine USING gin (lower(generic_name) gin_trgm_ops);
