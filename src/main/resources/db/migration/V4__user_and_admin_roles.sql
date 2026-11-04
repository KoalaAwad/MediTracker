-- Roles are just USER and ADMIN now. PATIENT becomes USER, DOCTOR goes away.
-- The patient table stays as every user's health record (it owns their prescriptions).

UPDATE roles SET role_name = 'USER', description = 'Regular user' WHERE role_name = 'PATIENT';
INSERT INTO roles (role_name, description)
SELECT 'USER', 'Regular user'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'USER');

-- user_role rows for DOCTOR go with it (ON DELETE CASCADE)
DELETE FROM roles WHERE role_name = 'DOCTOR';

-- everyone is a USER, admins included, so they can track their own medicines too
INSERT INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u CROSS JOIN roles r
WHERE r.role_name = 'USER'
  AND NOT EXISTS (SELECT 1 FROM user_role ur WHERE ur.user_id = u.user_id AND ur.role_id = r.role_id);

-- every user needs a patient record to hold their prescriptions
INSERT INTO patient (user_id, name, active)
SELECT u.user_id, u.name, TRUE
FROM users u
WHERE NOT EXISTS (SELECT 1 FROM patient p WHERE p.user_id = u.user_id);
UPDATE patient SET active = TRUE WHERE active = FALSE;

DROP TABLE IF EXISTS patient_doctor;
DROP TABLE IF EXISTS doctor;
