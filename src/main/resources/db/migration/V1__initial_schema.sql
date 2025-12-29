-- MediTracker Initial Schema
-- Flyway migration V1

-- ========================================
-- TABLES
-- ========================================

-- Users table
CREATE TABLE IF NOT EXISTS users (
    user_id SERIAL PRIMARY KEY,
    name VARCHAR(255),
    username VARCHAR(255),
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Roles table
CREATE TABLE IF NOT EXISTS roles (
    role_id SERIAL PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
);

-- User roles join table
CREATE TABLE IF NOT EXISTS user_role (
    user_id INTEGER NOT NULL,
    role_id INTEGER NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

-- Patient table
CREATE TABLE IF NOT EXISTS patient (
    patient_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL UNIQUE,
    name VARCHAR(255),
    date_of_birth DATE,
    gender VARCHAR(20),
    phone VARCHAR(30),
    address VARCHAR(255),
    blood_type VARCHAR(10),
    allergies TEXT,
    medical_history TEXT,
    active BOOLEAN DEFAULT true NOT NULL
);

-- Doctor table
CREATE TABLE IF NOT EXISTS doctor (
    doctor_id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL UNIQUE,
    first_name VARCHAR(120),
    last_name VARCHAR(120),
    specialization VARCHAR(150),
    license_number VARCHAR(100),
    phone VARCHAR(30),
    clinic_address VARCHAR(255),
    active BOOLEAN DEFAULT true NOT NULL
);

-- Patient-Doctor join table
CREATE TABLE IF NOT EXISTS patient_doctor (
    patient_id INTEGER NOT NULL,
    doctor_id INTEGER NOT NULL,
    PRIMARY KEY (patient_id, doctor_id)
);

-- Medicine table
CREATE TABLE IF NOT EXISTS medicine (
    medicine_id SERIAL PRIMARY KEY,
    medicine_name VARCHAR(255) NOT NULL,
    generic_name VARCHAR(255),
    manufacturer VARCHAR(255),
    active BOOLEAN DEFAULT true NOT NULL,
    openfda JSONB
);

-- Prescription table
CREATE TABLE IF NOT EXISTS prescription (
    prescription_id SERIAL PRIMARY KEY,
    patient_id INTEGER NOT NULL,
    medicine_id INTEGER NOT NULL,
    dosage_amount NUMERIC(10,2) NOT NULL,
    dosage_unit VARCHAR(32) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    time_zone VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    active BOOLEAN DEFAULT true NOT NULL
);

-- Prescription schedule table (ElementCollection)
CREATE TABLE IF NOT EXISTS prescription_schedule (
    prescription_id INTEGER NOT NULL,
    day_of_week VARCHAR(16) NOT NULL,
    time_of_day TIME NOT NULL,
    UNIQUE (prescription_id, day_of_week, time_of_day)
);

-- ========================================
-- FOREIGN KEYS
-- ========================================

ALTER TABLE user_role
    ADD CONSTRAINT user_role_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE user_role
    ADD CONSTRAINT user_role_role_id_fkey
    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE;

ALTER TABLE patient
    ADD CONSTRAINT patient_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE doctor
    ADD CONSTRAINT doctor_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE patient_doctor
    ADD CONSTRAINT patient_doctor_patient_id_fkey
    FOREIGN KEY (patient_id) REFERENCES patient(patient_id) ON DELETE CASCADE;

ALTER TABLE patient_doctor
    ADD CONSTRAINT patient_doctor_doctor_id_fkey
    FOREIGN KEY (doctor_id) REFERENCES doctor(doctor_id) ON DELETE CASCADE;

ALTER TABLE prescription
    ADD CONSTRAINT prescription_patient_id_fkey
    FOREIGN KEY (patient_id) REFERENCES patient(patient_id) ON DELETE CASCADE;

ALTER TABLE prescription
    ADD CONSTRAINT prescription_medicine_id_fkey
    FOREIGN KEY (medicine_id) REFERENCES medicine(medicine_id) ON DELETE RESTRICT;

ALTER TABLE prescription_schedule
    ADD CONSTRAINT prescription_schedule_prescription_id_fkey
    FOREIGN KEY (prescription_id) REFERENCES prescription(prescription_id) ON DELETE CASCADE;

-- ========================================
-- INDEXES
-- ========================================

CREATE INDEX IF NOT EXISTS idx_user_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_prescription_patient ON prescription(patient_id);
CREATE INDEX IF NOT EXISTS idx_prescription_medicine ON prescription(medicine_id);
CREATE INDEX IF NOT EXISTS idx_prescription_active ON prescription(active);
CREATE INDEX IF NOT EXISTS idx_medicine_active ON medicine(active);

-- ========================================
-- SEED DATA: Default Roles
-- ========================================

INSERT INTO roles (role_id, role_name, description) VALUES
    (1, 'ADMIN', 'System administrator with full access'),
    (2, 'DOCTOR', 'Medical doctor who can manage patients'),
    (3, 'PATIENT', 'Patient who can manage their own prescriptions')
ON CONFLICT (role_name) DO NOTHING;

-- Set sequence to correct value after manual inserts
SELECT setval('roles_role_id_seq', 3, true);

-- ========================================
-- SEED DATA: Default Admin User
-- ========================================

-- Insert default admin user (password: "password")
INSERT INTO users (email, password, name) VALUES
    ('admin@meditracker.com', '$2a$10$tWUSrGLFvkS2zn7btgkjruIVLM1HleSRq76Ky/MBle.Lb8nqyWwm2', 'Admin User')
ON CONFLICT (email) DO NOTHING;

-- Assign ADMIN and PATIENT roles to admin user
INSERT INTO user_role (user_id, role_id)
SELECT u.user_id, 1 FROM users u WHERE u.email = 'admin@meditracker.com'
ON CONFLICT DO NOTHING;

INSERT INTO user_role (user_id, role_id)
SELECT u.user_id, 3 FROM users u WHERE u.email = 'admin@meditracker.com'
ON CONFLICT DO NOTHING;

-- Create patient profile for admin user
INSERT INTO patient (user_id, name, date_of_birth, phone, address, active)
SELECT u.user_id, 'Admin User', '1990-01-01', '000-000-0000', 'Admin Address', true
FROM users u WHERE u.email = 'admin@meditracker.com'
ON CONFLICT (user_id) DO NOTHING;

