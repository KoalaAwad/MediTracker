-- MediTracker
-- Flyway migration V2

-- The default admin from V1 (admin@meditracker.com / "password") is public in the repo.
-- Remove it if its password was never changed. The first admin is now created at startup
-- from ADMIN_EMAIL / ADMIN_PASSWORD (see AdminBootstrap).
-- user_role, patient and the patient's prescriptions go with it via ON DELETE CASCADE.
DELETE FROM users
WHERE email = 'admin@meditracker.com'
  AND password = '$2a$10$tWUSrGLFvkS2zn7btgkjruIVLM1HleSRq76Ky/MBle.Lb8nqyWwm2';
