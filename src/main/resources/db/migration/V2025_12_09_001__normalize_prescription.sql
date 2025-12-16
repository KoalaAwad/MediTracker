-- =============================================
-- Normalize prescription schema (idempotent)
-- Dosage as value object, explicit schedule entries, timezone, auditing
-- =============================================

-- 1) Add dosage, dates, timezone, and auditing columns
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='prescription' AND column_name='dosage_amount') THEN
        ALTER TABLE prescription ADD COLUMN dosage_amount NUMERIC(10,2) NOT NULL DEFAULT 1.00;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='prescription' AND column_name='dosage_unit') THEN
        ALTER TABLE prescription ADD COLUMN dosage_unit VARCHAR(32) NOT NULL DEFAULT 'TABLET';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='prescription' AND column_name='start_date') THEN
        ALTER TABLE prescription ADD COLUMN start_date DATE NOT NULL DEFAULT CURRENT_DATE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='prescription' AND column_name='end_date') THEN
        ALTER TABLE prescription ADD COLUMN end_date DATE NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='prescription' AND column_name='time_zone') THEN
        ALTER TABLE prescription ADD COLUMN time_zone VARCHAR(64) NOT NULL DEFAULT 'UTC';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='prescription' AND column_name='created_at') THEN
        ALTER TABLE prescription ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='prescription' AND column_name='updated_at') THEN
        ALTER TABLE prescription ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
    END IF;
END $$;

-- 2) Add date range constraint safely
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints tc
        JOIN information_schema.constraint_column_usage ccu
          ON tc.constraint_name = ccu.constraint_name
        WHERE tc.table_name='prescription'
          AND tc.constraint_type='CHECK'
          AND tc.constraint_name='prescription_date_range_chk'
    ) THEN
        ALTER TABLE prescription
          ADD CONSTRAINT prescription_date_range_chk
          CHECK (end_date IS NULL OR end_date >= start_date);
    END IF;
END $$;

-- 3) Create schedule table
CREATE TABLE IF NOT EXISTS prescription_schedule (
    prescription_id INT NOT NULL REFERENCES prescription(prescription_id) ON DELETE CASCADE,
    day_of_week VARCHAR(16) NOT NULL,
    time_of_day TIME NOT NULL,
    PRIMARY KEY (prescription_id, day_of_week, time_of_day)
);

-- 4) Backfill schedule entries only if legacy columns exist
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='prescription'
          AND column_name IN ('week_days', 'day_of_week_pattern')
    ) THEN
        INSERT INTO prescription_schedule (prescription_id, day_of_week, time_of_day)
        SELECT p.prescription_id,
               TRIM(UPPER(day_token)) AS day_of_week,
               p.time_of_day
        FROM prescription p
        CROSS JOIN LATERAL unnest(string_to_array(
            COALESCE(p.week_days, p.day_of_week_pattern, ''), ','
        )) AS day_token
        WHERE p.time_of_day IS NOT NULL
          AND COALESCE(p.week_days, p.day_of_week_pattern) IS NOT NULL
        ON CONFLICT DO NOTHING;
    END IF;
END $$;

-- 5) Indexes
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE c.relname='idx_prescription_patient') THEN
        CREATE INDEX idx_prescription_patient ON prescription(patient_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE c.relname='idx_prescription_medicine') THEN
        CREATE INDEX idx_prescription_medicine ON prescription(medicine_id);
    END IF;
END $$;

-- 6) Drop legacy columns safely (after backfill)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='prescription' AND column_name='week_days') THEN
        ALTER TABLE prescription DROP COLUMN week_days;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='prescription' AND column_name='day_of_week_pattern') THEN
        ALTER TABLE prescription DROP COLUMN day_of_week_pattern;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='prescription' AND column_name='time_of_day') THEN
        ALTER TABLE prescription DROP COLUMN time_of_day;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='prescription' AND column_name='frequency_type') THEN
        ALTER TABLE prescription DROP COLUMN frequency_type;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='prescription' AND column_name='interval_value') THEN
        ALTER TABLE prescription DROP COLUMN interval_value;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='prescription' AND column_name='interval_type') THEN
        ALTER TABLE prescription DROP COLUMN interval_type;
    END IF;
END $$;

