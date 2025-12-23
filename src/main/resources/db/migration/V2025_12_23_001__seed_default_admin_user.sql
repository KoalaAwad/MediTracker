-- Create default admin user on initial setup
-- Email: admin@meditracker.com
-- Password: password (BCrypt hashed)

-- Insert the admin user (only if not exists)
INSERT INTO users (email, password, name)
SELECT 'admin@meditracker.com', '$2a$10$CwTycUXWue0Thq9StjUM0uJ8C8kmvghqfjVDw1jXfz0L9Y0p9Y7Eq', 'Admin User'
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'admin@meditracker.com'
);

-- Get the user_id for the admin user
DO $$
DECLARE
    admin_user_id INT;
    admin_role_id INT;
    patient_role_id INT;
BEGIN
    -- Get user ID
    SELECT user_id INTO admin_user_id FROM users WHERE email = 'admin@meditracker.com';

    -- Get role IDs
    SELECT role_id INTO admin_role_id FROM roles WHERE role_name = 'ADMIN';
    SELECT role_id INTO patient_role_id FROM roles WHERE role_name = 'PATIENT';

    -- Insert ADMIN role if not exists
    IF NOT EXISTS (
        SELECT 1 FROM user_roles WHERE user_id = admin_user_id AND role_id = admin_role_id
    ) THEN
        INSERT INTO user_roles (user_id, role_id) VALUES (admin_user_id, admin_role_id);
    END IF;

    -- Insert PATIENT role if not exists
    IF NOT EXISTS (
        SELECT 1 FROM user_roles WHERE user_id = admin_user_id AND role_id = patient_role_id
    ) THEN
        INSERT INTO user_roles (user_id, role_id) VALUES (admin_user_id, patient_role_id);
    END IF;

    -- Create patient profile for the admin user if not exists
    IF NOT EXISTS (
        SELECT 1 FROM patient WHERE user_id = admin_user_id
    ) THEN
        INSERT INTO patient (user_id, date_of_birth, address, phone_number)
        VALUES (admin_user_id, '1990-01-01', 'Admin Address', '000-000-0000');
    END IF;
END $$;

